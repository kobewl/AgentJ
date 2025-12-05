package com.wangliang.agentj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for Lynxe API endpoints
 *
 * @author Lynxe Team
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins("*") // Allow all origins
			.allowedMethods("*") // Allow all HTTP methods
			.allowedHeaders("*") // Allow all headers
			.allowCredentials(false); // Note: when origins="*", credentials must be false
	}

}
