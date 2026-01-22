package com.wangliang.agentj.planning.initializer;

import com.wangliang.agentj.planning.service.PlanTemplateInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Startup initializer for plan templates from startup-plans directory Also registers
 * default plan templates as coordinator tools (internal toolcalls)
 */
@Component
public class PlanTemplateStartupInitializer {

	private static final Logger log = LoggerFactory.getLogger(PlanTemplateStartupInitializer.class);

	private static final String DEFAULT_LANGUAGE = "en";

	@Autowired
	private PlanTemplateInitializationService planTemplateInitializationService;

	@Value("${namespace.value}")
	private String namespace;

	/**
	 * Initialize startup plan templates when application is ready
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void initializeStartupPlanTemplates() {
		log.info("Starting startup plan templates initialization for namespace: {}", namespace);

		try {
			// Initialize all plan templates with toolConfig and create coordinator tools
			planTemplateInitializationService.initializePlanTemplatesForNamespaceWithLanguage(namespace,
					DEFAULT_LANGUAGE);
			log.info("Completed startup plan templates initialization for namespace: {}", namespace);
		}
		catch (Exception e) {
			log.error("Failed to initialize startup plan templates for namespace: {}", namespace, e);
		}
	}

}
