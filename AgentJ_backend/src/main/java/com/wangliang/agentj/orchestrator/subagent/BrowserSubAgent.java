package com.wangliang.agentj.orchestrator.subagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.orchestrator.SubAgent;
import com.wangliang.agentj.orchestrator.model.AgentContext;
import com.wangliang.agentj.orchestrator.model.AgentResult;
import com.wangliang.agentj.orchestrator.model.AgentTask;
import com.wangliang.agentj.tools.browser.BrowserUseTool;
import com.wangliang.agentj.tools.browser.ChromeDriverService;
import com.wangliang.agentj.tools.browser.actions.BrowserRequestVO;
import com.wangliang.agentj.tools.code.ToolExecuteResult;
import com.wangliang.agentj.tools.i18n.ToolI18nService;
import com.wangliang.agentj.tools.innerStorage.SmartContentSavingService;
import com.wangliang.agentj.tools.shortUrl.ShortUrlService;
import com.wangliang.agentj.tools.textOperator.TextFileService;
import org.springframework.stereotype.Component;

@Component
public class BrowserSubAgent implements SubAgent {

	private final ChromeDriverService chromeDriverService;
	private final SmartContentSavingService innerStorageService;
	private final ObjectMapper objectMapper;
	private final ShortUrlService shortUrlService;
	private final TextFileService textFileService;
	private final ToolI18nService toolI18nService;

	public BrowserSubAgent(ChromeDriverService chromeDriverService,
			SmartContentSavingService innerStorageService,
			ObjectMapper objectMapper,
			ShortUrlService shortUrlService,
			TextFileService textFileService,
			ToolI18nService toolI18nService) {
		this.chromeDriverService = chromeDriverService;
		this.innerStorageService = innerStorageService;
		this.objectMapper = objectMapper;
		this.shortUrlService = shortUrlService;
		this.textFileService = textFileService;
		this.toolI18nService = toolI18nService;
	}

	@Override
	public String name() {
		return "browser";
	}

	@Override
	public boolean canHandle(AgentTask task) {
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase();
		String intent = task.getIntent() == null ? "" : task.getIntent().toLowerCase();
		return intent.contains("browser") || intent.contains("web")
				|| message.contains("浏览器") || message.contains("网页")
				|| message.contains("打开网站") || message.contains("抓取");
	}

	@Override
	public AgentResult execute(AgentTask task, AgentContext context) {
		BrowserUseTool tool = BrowserUseTool.getInstance(chromeDriverService, innerStorageService, objectMapper,
				shortUrlService, textFileService, toolI18nService);
		tool.setCurrentPlanId(task.getId());
		tool.setRootPlanId(task.getId());

		BrowserRequestVO request = new BrowserRequestVO();
		String action = getString(task, "action", null);
		String url = getString(task, "url", null);
		if (action == null || action.isBlank()) {
			if (url != null && !url.isBlank()) {
				action = "navigate";
			}
			else {
				action = "get_text";
			}
		}

		request.setAction(action);
		request.setUrl(url);
		request.setIndex(getInteger(task, "index"));
		request.setText(getString(task, "text", null));
		request.setScript(getString(task, "script", null));
		request.setScrollAmount(getInteger(task, "scroll_amount"));
		if (request.getScrollAmount() == null) {
			request.setScrollAmount(getInteger(task, "scrollAmount"));
		}
		request.setDirection(getString(task, "direction", null));
		request.setTabId(getInteger(task, "tab_id"));
		if (request.getTabId() == null) {
			request.setTabId(getInteger(task, "tabId"));
		}
		request.setElementName(getString(task, "element_name", null));
		if (request.getElementName() == null) {
			request.setElementName(getString(task, "elementName", null));
		}
		request.setPositionX(getDouble(task, "position_x"));
		if (request.getPositionX() == null) {
			request.setPositionX(getDouble(task, "positionX"));
		}
		request.setPositionY(getDouble(task, "position_y"));
		if (request.getPositionY() == null) {
			request.setPositionY(getDouble(task, "positionY"));
		}

		ToolExecuteResult result = tool.run(request);
		AgentResult agentResult = AgentResult.success(name(), result == null ? "" : result.getOutput());
		agentResult.getMetadata().put("action", action);
		agentResult.getMetadata().put("url", url);
		return agentResult;
	}

	@Override
	public int priority() {
		return 5;
	}

	private String getString(AgentTask task, String key, String fallback) {
		Object value = task.getParameters().get(key);
		if (value == null) {
			return fallback;
		}
		String text = value.toString();
		return text.isBlank() ? fallback : text;
	}

	private Integer getInteger(AgentTask task, String key) {
		Object value = task.getParameters().get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private Double getDouble(AgentTask task, String key) {
		Object value = task.getParameters().get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
