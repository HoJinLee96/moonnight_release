package net.chamman.moonnight.rate.limiter;

public enum RateLimitKeyGenerator {
	
	VERIFICATION_CODE_REQUEST("rate_limit:verification_code_request:", 10, 30),
	ESTIMATE_REGISTER("rate_limit:estimate_register:", 5, 30),
	QUESTION_REGISTER("rate_limit:question_register:", 5, 30),
	REQUEST_CLIENT_IP("rate_limit:request_client_ip:", 30, 30);

	private final String prefix;
	private final int maxRequest;
	private final int timeoutMinutes;

	RateLimitKeyGenerator(String prefix, int maxRequest, int timeoutMinutes) {
		this.prefix = prefix;
		this.maxRequest = maxRequest;
		this.timeoutMinutes = timeoutMinutes;
	}

	public String key(String id) {
		return prefix + id;
	}

	public int getMaxRequest() {
		return maxRequest;
	}

	public int getTimeoutMinutes() {
		return timeoutMinutes;
	}
}
