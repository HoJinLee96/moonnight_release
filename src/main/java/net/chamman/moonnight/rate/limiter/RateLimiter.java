package net.chamman.moonnight.rate.limiter;

public interface RateLimiter {
    boolean isAllowed(String key, int maxCount);
}
