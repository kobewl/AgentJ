/*
 * A tolerant SSE transport for MCP HTTP servers.
 *
 * Differences from the upstream WebFluxSseClientTransport:
 * - Duplicate or malformed endpoint events will not immediately fail the stream.
 * - Logs the emit result to help diagnose server-specific behaviors (e.g., multiple
 *   endpoint events or missing data).
 * - Supports servers like DashScope that send endpoint event with a different message URL.
 * - Properly waits for endpoint event before sending messages.
 */
package com.wangliang.agentj.mcp.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * SSE transport that tolerates duplicate/missing endpoint events from servers
 * like DashScope, avoiding hard failures during the handshake.
 * 
 * For DashScope and similar cloud MCP services, this transport:
 * 1. Waits for the endpoint event from the SSE stream
 * 2. Uses the received endpoint URL for sending messages
 * 3. Falls back to SSE endpoint if no endpoint event received within timeout
 */
public class TolerantWebFluxSseClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(TolerantWebFluxSseClientTransport.class);

	private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

	private static final String MESSAGE_EVENT_TYPE = "message";

	private static final String ENDPOINT_EVENT_TYPE = "endpoint";

	/**
	 * Timeout for waiting for the endpoint event from the server.
	 */
	private static final Duration ENDPOINT_WAIT_TIMEOUT = Duration.ofSeconds(30);

	private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {
	};

	private final WebClient webClient;

	private final McpJsonMapper jsonMapper;

	private final String sseEndpoint;

	/**
	 * Fallback endpoint to use for message posting if no endpoint event is
	 * received.
	 */
	private final String fallbackMessageEndpoint;

	/**
	 * The actual message endpoint received from the server (via endpoint event).
	 * This is set when we receive the endpoint event from the SSE stream.
	 */
	private final AtomicReference<String> messageEndpoint = new AtomicReference<>();

	/**
	 * CompletableFuture that completes when the endpoint is set.
	 * Used to wait for the endpoint before sending messages.
	 */
	private final CompletableFuture<String> endpointFuture = new CompletableFuture<>();

	private final Retry inboundRetry;

	private volatile boolean isClosing;

	private Disposable inboundSubscription;

	public TolerantWebFluxSseClientTransport(WebClient.Builder webClientBuilder, JacksonMcpJsonMapper jsonMapper,
			String sseEndpoint) {
		this(webClientBuilder, jsonMapper, sseEndpoint, null);
	}

	/**
	 * Constructor with optional fallback message endpoint.
	 * 
	 * @param webClientBuilder        WebClient builder
	 * @param jsonMapper              JSON mapper
	 * @param sseEndpoint             SSE endpoint for receiving events
	 * @param fallbackMessageEndpoint Fallback endpoint for sending messages (null =
	 *                                use sseEndpoint)
	 */
	public TolerantWebFluxSseClientTransport(WebClient.Builder webClientBuilder, JacksonMcpJsonMapper jsonMapper,
			String sseEndpoint, String fallbackMessageEndpoint) {
		this.webClient = Objects.requireNonNull(webClientBuilder, "WebClient.Builder must not be null").build();
		this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
		if (sseEndpoint == null || sseEndpoint.isBlank()) {
			throw new IllegalArgumentException("SSE endpoint must not be null or empty");
		}
		this.sseEndpoint = sseEndpoint;
		this.fallbackMessageEndpoint = (fallbackMessageEndpoint != null && !fallbackMessageEndpoint.isBlank())
				? fallbackMessageEndpoint
				: sseEndpoint;

		logger.info("TolerantWebFluxSseClientTransport initialized - sseEndpoint: {}, fallbackEndpoint: {}",
				sseEndpoint, this.fallbackMessageEndpoint);

		// Retry on connection errors only (not on normal stream completion)
		this.inboundRetry = Retry.backoff(3, Duration.ofSeconds(2))
				.maxBackoff(Duration.ofSeconds(10))
				.filter(ex -> {
					if (isClosing) {
						return false;
					}
					// Only retry on actual connection/IO errors
					boolean shouldRetry = ex instanceof IOException
							|| ex instanceof WebClientResponseException
							|| (ex.getCause() != null && ex.getCause() instanceof IOException);
					if (shouldRetry) {
						logger.warn("SSE connection error (will retry): {} - {}",
								ex.getClass().getSimpleName(), ex.getMessage());
					}
					return shouldRetry;
				})
				.doBeforeRetry(rs -> {
					logger.info("Retrying SSE connection, attempt {}/3, error: {}",
							rs.totalRetries() + 1, rs.failure().getMessage());
				})
				.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
					logger.error("SSE connection failed after {} retries. Last error: {}",
							retrySignal.totalRetries(),
							retrySignal.failure().getMessage());
					return retrySignal.failure();
				});
	}

	@Override
	public List<String> protocolVersions() {
		return List.of(MCP_PROTOCOL_VERSION);
	}

	@Override
	public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> inboundHandler) {
		logger.info("Connecting to SSE endpoint: {}", sseEndpoint);

		// Start SSE event stream
		Flux<McpSchema.JSONRPCMessage> handledFlux = eventStream()
				.doOnSubscribe(s -> logger.debug("SSE stream subscribed"))
				.doOnNext(event -> {
					String eventType = event.event();
					logger.debug("SSE event received - type: {}, hasData: {}", eventType, event.data() != null);
				})
				.concatMap(event -> Mono.just(event).<McpSchema.JSONRPCMessage>handle(this::handleEvent)
						.transform(inboundHandler))
				.doOnError(error -> {
					if (!isClosing) {
						logger.warn("SSE stream error: {}", error.getMessage());
					}
				})
				.doOnComplete(() -> logger.debug("SSE stream completed"));

		this.inboundSubscription = handledFlux
				.subscribeOn(Schedulers.boundedElastic())
				.subscribe(
						msg -> logger.debug("Processed message: {}", msg.getClass().getSimpleName()),
						error -> {
							if (!isClosing) {
								logger.error("SSE subscription error: {}", error.getMessage());
								// Complete connect with error if endpoint not yet set
								endpointFuture.completeExceptionally(error);
							}
						},
						() -> logger.debug("SSE subscription completed"));

		// Wait for endpoint event with timeout, then use fallback if needed
		return Mono.fromFuture(() -> endpointFuture.orTimeout(ENDPOINT_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
				.doOnSuccess(endpoint -> logger.info("Endpoint received from server: {}", endpoint))
				.onErrorResume(ex -> {
					if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
						logger.info("No endpoint event received within {}s, using fallback: {}",
								ENDPOINT_WAIT_TIMEOUT.getSeconds(), fallbackMessageEndpoint);
						setEndpointIfNotSet(fallbackMessageEndpoint);
						return Mono.just(fallbackMessageEndpoint);
					}
					return Mono.error(ex);
				})
				.then();
	}

	/**
	 * Set the message endpoint if not already set.
	 * Thread-safe using compareAndSet.
	 */
	private void setEndpointIfNotSet(String endpoint) {
		if (messageEndpoint.compareAndSet(null, endpoint)) {
			logger.info("Message endpoint set to: {}", endpoint);
			endpointFuture.complete(endpoint);
		}
	}

	private void handleEvent(ServerSentEvent<String> event, SynchronousSink<McpSchema.JSONRPCMessage> sink) {
		String eventType = event.event();
		String data = event.data();

		logger.debug("Handling SSE event - type: {}, data length: {}",
				eventType, data != null ? data.length() : 0);

		// Handle endpoint event
		if (ENDPOINT_EVENT_TYPE.equals(eventType)) {
			if (data == null || data.isBlank()) {
				logger.warn("Received endpoint event with empty data");
				sink.complete();
				return;
			}

			// Set the endpoint - this will complete the connect() future
			String previousEndpoint = messageEndpoint.getAndSet(data);
			if (previousEndpoint == null) {
				logger.info("Received endpoint from server: {}", data);
				endpointFuture.complete(data);
			} else {
				logger.debug("Duplicate endpoint event ignored (previous: {}, new: {})", previousEndpoint, data);
			}
			sink.complete();
			return;
		}

		// Handle message event
		if (MESSAGE_EVENT_TYPE.equals(eventType)) {
			if (data == null || data.isBlank()) {
				logger.debug("Received message event with empty data");
				sink.complete();
				return;
			}
			try {
				McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, data);
				logger.debug("Parsed message event: {}", message.getClass().getSimpleName());
				sink.next(message);
			} catch (IOException e) {
				logger.error("Failed to parse message event: {}", e.getMessage());
				sink.error(e);
			}
			return;
		}

		// Handle events with no type or unknown type - try to parse as JSON-RPC message
		if (data != null && !data.isBlank()) {
			String trimmedData = data.trim();
			if (trimmedData.startsWith("{")) {
				try {
					McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, data);
					logger.debug("Parsed untyped event as JSON-RPC message: {}", message.getClass().getSimpleName());
					sink.next(message);
					return;
				} catch (IOException e) {
					logger.debug("Failed to parse untyped event as JSON-RPC: {}", e.getMessage());
				}
			}
		}

		logger.debug("Ignoring unrecognized SSE event - type: {}", eventType);
		sink.complete();
	}

	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage jsonRpcMessage) {
		return Mono.defer(() -> {
			// Wait for endpoint to be set (with timeout)
			return Mono
					.fromFuture(() -> endpointFuture.orTimeout(ENDPOINT_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
					.onErrorResume(ex -> {
						if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
							logger.warn("Timeout waiting for endpoint, using fallback: {}", fallbackMessageEndpoint);
							setEndpointIfNotSet(fallbackMessageEndpoint);
							return Mono.just(fallbackMessageEndpoint);
						}
						return Mono.error(ex);
					});
		}).flatMap(endpoint -> {
			try {
				String json = jsonMapper.writeValueAsString(jsonRpcMessage);
				String method = extractMethod(json);
				logger.info("Sending MCP request - method: {}, endpoint: {}", method, endpoint);
				logger.debug("Request payload length: {}", json.length());

				return webClient.post()
						.uri(endpoint)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
						.header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
						.bodyValue(json)
						.exchangeToMono(response -> {
							HttpStatusCode status = response.statusCode();
							logger.debug("Response status: {}", status);

							if (status.is2xxSuccessful()) {
								return response.bodyToMono(String.class)
										.defaultIfEmpty("")
										.doOnNext(body -> {
											if (!body.isEmpty()) {
												logger.debug("Response body (truncated): {}",
														body.length() > 200 ? body.substring(0, 200) + "..." : body);
											}
										})
										.then();
							} else {
								return response.bodyToMono(String.class)
										.defaultIfEmpty("No error body")
										.flatMap(errorBody -> {
											logger.error("HTTP error {} from {}: {}", status, endpoint, errorBody);
											return Mono.error(new RuntimeException(
													"HTTP error " + status + ": " + errorBody));
										});
							}
						})
						.doOnSuccess(v -> logger.debug("Message sent successfully"))
						.doOnError(error -> {
							if (!isClosing) {
								logger.error("Error sending message: {}", error.getMessage());
							}
						});
			} catch (IOException e) {
				if (isClosing) {
					return Mono.empty();
				}
				logger.error("Failed to serialize message: {}", e.getMessage());
				return Mono.error(new RuntimeException("Failed to serialize message", e));
			}
		});
	}

	/**
	 * Extract method name from JSON for logging
	 */
	private String extractMethod(String json) {
		try {
			int methodIndex = json.indexOf("\"method\"");
			if (methodIndex != -1) {
				int colonIndex = json.indexOf(":", methodIndex);
				int quoteStart = json.indexOf("\"", colonIndex);
				int quoteEnd = json.indexOf("\"", quoteStart + 1);
				if (quoteStart != -1 && quoteEnd != -1) {
					return json.substring(quoteStart + 1, quoteEnd);
				}
			}
		} catch (Exception e) {
			// Ignore parsing errors
		}
		return "unknown";
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			logger.info("Closing SSE transport gracefully");
			isClosing = true;
			if (inboundSubscription != null) {
				inboundSubscription.dispose();
			}
			// Complete the future if still pending
			endpointFuture.complete(fallbackMessageEndpoint);
		}).then().subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public <T> T unmarshalFrom(Object value, io.modelcontextprotocol.json.TypeRef<T> typeRef) {
		return jsonMapper.convertValue(value, typeRef);
	}

	protected Flux<ServerSentEvent<String>> eventStream() {
		// Ensure endpoint starts with / for proper URL resolution
		String normalizedEndpoint = sseEndpoint.startsWith("/") ? sseEndpoint : "/" + sseEndpoint;
		logger.info("Creating SSE event stream for endpoint: {}", normalizedEndpoint);

		return webClient.get()
				.uri(normalizedEndpoint)
				.accept(MediaType.TEXT_EVENT_STREAM)
				.header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
				.retrieve()
				.onStatus(status -> !status.is2xxSuccessful(), response -> {
					return response.bodyToMono(String.class)
							.defaultIfEmpty("No error body")
							.flatMap(body -> {
								logger.error("SSE connection failed with status {}: {}",
										response.statusCode(), body);
								return Mono.error(new RuntimeException(
										"SSE connection failed: " + response.statusCode() + " - " + body));
							});
				})
				.bodyToFlux(SSE_TYPE)
				.doOnSubscribe(s -> logger.info("SSE stream subscribed to {}", normalizedEndpoint))
				.doOnNext(event -> logger.debug("SSE raw event: type={}, id={}, data={}",
						event.event(), event.id(),
						event.data() != null ? event.data().substring(0, Math.min(100, event.data().length())) : null))
				.doOnError(error -> logger.error("SSE stream error: {} - {}",
						error.getClass().getSimpleName(), error.getMessage()))
				.doOnComplete(() -> logger.info("SSE stream completed normally"))
				.doOnCancel(() -> logger.info("SSE stream cancelled"))
				.retryWhen(inboundRetry);
	}

}
