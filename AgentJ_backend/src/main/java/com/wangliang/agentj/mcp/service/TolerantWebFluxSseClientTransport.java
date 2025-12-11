/*
 * A tolerant SSE transport for MCP HTTP servers.
 *
 * Differences from the upstream WebFluxSseClientTransport:
 * - Duplicate or malformed endpoint events will not immediately fail the stream.
 * - Logs the emit result to help diagnose server-specific behaviors (e.g., multiple
 *   endpoint events or missing data).
 */
package com.wangliang.agentj.mcp.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * SSE transport that tolerates duplicate/missing endpoint events from servers like
 * DashScope, avoiding hard failures during the handshake.
 */
public class TolerantWebFluxSseClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(TolerantWebFluxSseClientTransport.class);

	private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

	private static final String MESSAGE_EVENT_TYPE = "message";

	private static final String ENDPOINT_EVENT_TYPE = "endpoint";

	private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {
	};

	private final WebClient webClient;

	private final McpJsonMapper jsonMapper;

	private final Sinks.One<String> messageEndpointSink = Sinks.one();

	private final String sseEndpoint;

	private final Retry inboundRetry;

	private volatile boolean isClosing;

	private Disposable inboundSubscription;

	public TolerantWebFluxSseClientTransport(WebClient.Builder webClientBuilder, JacksonMcpJsonMapper jsonMapper,
			String sseEndpoint) {
		this.webClient = Objects.requireNonNull(webClientBuilder, "WebClient.Builder must not be null").build();
		this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
		if (sseEndpoint == null || sseEndpoint.isBlank()) {
			throw new IllegalArgumentException("SSE endpoint must not be null or empty");
		}
		this.sseEndpoint = sseEndpoint;

		// Retry on IO errors; otherwise propagate. Keeps retrying while not closing.
		this.inboundRetry = Retry.indefinitely().filter(ex -> ex instanceof IOException).doBeforeRetry(rs -> {
			if (!isClosing) {
				logger.debug("Retrying SSE connection after IO error, attempt {}", rs.totalRetries() + 1);
			}
		});
	}

	@Override
	public List<String> protocolVersions() {
		return List.of(MCP_PROTOCOL_VERSION);
	}

	@Override
	public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> inboundHandler) {
		Flux<McpSchema.JSONRPCMessage> handledFlux = eventStream().concatMap(event -> Mono.just(event)
			.<McpSchema.JSONRPCMessage>handle(this::handleEvent)
			.transform(inboundHandler));

		this.inboundSubscription = handledFlux.subscribe();

		// Wait until we have the message endpoint (set by the SSE "endpoint" event)
		return messageEndpointSink.asMono().then();
	}

	private void handleEvent(ServerSentEvent<String> event, SynchronousSink<McpSchema.JSONRPCMessage> sink) {
		String eventType = event.event();

		if (ENDPOINT_EVENT_TYPE.equals(eventType)) {
			String endpoint = event.data();
			if (endpoint == null || endpoint.isBlank()) {
				sink.error(new RuntimeException("Missing SSE endpoint data"));
				return;
			}
			Sinks.EmitResult emitResult = messageEndpointSink.tryEmitValue(endpoint);
			if (emitResult.isSuccess()) {
				sink.complete();
			}
			else if (emitResult == Sinks.EmitResult.FAIL_TERMINATED) {
				// Duplicate endpoint event; ignore to avoid hard failure
				logger.debug("Duplicate SSE endpoint event ignored");
				sink.complete();
			}
			else {
				logger.error("Failed to handle SSE endpoint event, emitResult={}", emitResult);
				sink.error(new RuntimeException("Failed to handle SSE endpoint event, emitResult=" + emitResult));
			}
			return;
		}

		if (MESSAGE_EVENT_TYPE.equals(eventType)) {
			try {
				McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, event.data());
				sink.next(message);
			}
			catch (IOException e) {
				sink.error(e);
			}
			return;
		}

		logger.debug("Received unrecognized SSE event type: {}", eventType);
		sink.complete();
	}

	@Override
	public Mono<Void> sendMessage(McpSchema.JSONRPCMessage jsonRpcMessage) {
		return messageEndpointSink.asMono().flatMap(endpoint -> {
			try {
				String json = jsonMapper.writeValueAsString(jsonRpcMessage);
				return webClient.post()
					.uri(endpoint, new Object[0])
					.contentType(MediaType.APPLICATION_JSON)
					.header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
					.bodyValue(json)
					.retrieve()
					.toBodilessEntity()
					.doOnSuccess(response -> logger.debug("Message sent successfully"))
					.doOnError(error -> {
						if (!isClosing) {
							logger.error("Error sending message: {}", error.getMessage());
						}
					})
					.then();
			}
			catch (IOException e) {
				if (isClosing) {
					return Mono.empty();
				}
				return Mono.error(new RuntimeException("Failed to serialize message", e));
			}
		});
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			isClosing = true;
			if (inboundSubscription != null) {
				inboundSubscription.dispose();
			}
		}).then().subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public <T> T unmarshalFrom(Object value, io.modelcontextprotocol.json.TypeRef<T> typeRef) {
		return jsonMapper.convertValue(value, typeRef);
	}

	protected reactor.core.publisher.Flux<ServerSentEvent<String>> eventStream() {
		return webClient.get()
			.uri(sseEndpoint, new Object[0])
			.accept(MediaType.TEXT_EVENT_STREAM)
			.header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
			.retrieve()
			.bodyToFlux(SSE_TYPE)
			.retryWhen(inboundRetry);
	}

}
