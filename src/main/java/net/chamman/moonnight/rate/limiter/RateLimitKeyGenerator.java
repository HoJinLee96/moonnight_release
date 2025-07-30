package net.chamman.moonnight.rate.limiter;

public enum RateLimitKeyGenerator {
	VERIFY_PHONE("rate_limit:verify:phone:", 20),
	VERIFY_EMAIL("rate_limit:verify:email:", 20),
	ESTIMATE("rate_limit:estimate:", 5),
	IP("rate_limit:ip:", 40);

	private final String prefix;
	private final int maxRequest;

	RateLimitKeyGenerator(String prefix, int maxRequest) {
		this.prefix = prefix;
		this.maxRequest = maxRequest;
	}

	public String key(String id) {
		return prefix + id;
	}

	public int getMaxRequest() {
		return maxRequest;
	}
}
