package com.wangliang.agentj.config;

/**
 * 缓存条目（Cache Entry）
 *
 * key -> ConfigCacheEntry<T>
 * @param <T>
 */
public class ConfigCacheEntry<T> {

	private T value;

	private long lastUpdateTime;

	private static final long EXPIRATION_TIME = 30000; // 30 seconds expiration

	public ConfigCacheEntry(T value) {
		this.value = value;
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public boolean isExpired() {
		return System.currentTimeMillis() - lastUpdateTime > EXPIRATION_TIME;
	}

}
