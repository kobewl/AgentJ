/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangliang.agentj.tools.browser.actions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.wangliang.agentj.tools.browser.BrowserUseTool;
import com.wangliang.agentj.tools.code.ToolExecuteResult;

public class ClickByElementAction extends BrowserAction {

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClickByElementAction.class);

	public ClickByElementAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer index = request.getIndex();
		if (index == null) {
			return new ToolExecuteResult("Index is required for 'click' action");
		}

		// Check if element exists
		if (!elementExistsByIdx(index)) {
			return new ToolExecuteResult("Element with index " + index + " not found in ARIA snapshot");
		}

		Page page = getCurrentPage();
		waitForPageReady(page);
		Locator locator = getLocatorByIdx(index);
		if (locator == null) {
			return new ToolExecuteResult("Failed to create locator for element with index " + index);
		}

		String clickResultMessage = clickAndSwitchToNewTabIfOpened(page, () -> {
			try {
				// Use a reasonable timeout for element operations (max 10 seconds)
				int elementTimeout = getElementTimeoutMs();
				log.debug("Using element timeout: {}ms for click operations", elementTimeout);

				// Wait for element to be visible before clicking
				locator.waitFor(new Locator.WaitForOptions()
					.setTimeout(elementTimeout)
					.setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
				try {
					locator.scrollIntoViewIfNeeded(
							new Locator.ScrollIntoViewIfNeededOptions().setTimeout(elementTimeout));
				}
				catch (Exception e) {
					log.debug("Scroll into view failed for idx {}: {}", index, e.getMessage());
				}

				// Click with explicit timeout (fallback to force click if needed)
				try {
					locator.click(new Locator.ClickOptions().setTimeout(elementTimeout));
				}
				catch (com.microsoft.playwright.TimeoutError e) {
					log.error("Timeout waiting for element with idx {} to be ready for click: {}", index,
							e.getMessage());
					throw e;
				}
				catch (com.microsoft.playwright.PlaywrightException e) {
					log.warn("Standard click failed for idx {}, attempting force click: {}", index, e.getMessage());
					try {
						locator.click(new Locator.ClickOptions().setTimeout(elementTimeout).setForce(true));
					}
					catch (Exception forceClickError) {
						log.warn("Force click failed for idx {}, attempting JS click: {}", index,
								forceClickError.getMessage());
						locator.evaluate("el => el.click()");
					}
				}

				// Add small delay to ensure the action is processed
				Thread.sleep(500);

			}
			catch (com.microsoft.playwright.TimeoutError e) {
				log.error("Timeout waiting for element with idx {} to be ready for click: {}", index, e.getMessage());
				throw e;
			}
			catch (Exception e) {
				log.error("Error during click on element with idx {}: {}", index, e.getMessage());
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException("Error clicking element: " + e.getMessage(), e);
			}
		});
		return new ToolExecuteResult("Successfully clicked element at index " + index + " " + clickResultMessage);
	}

}
