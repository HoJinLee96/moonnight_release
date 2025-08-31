package net.chamman.moonnight.rate.limiter.impl;

import static net.chamman.moonnight.global.exception.HttpStatusCode.TOO_MANY_REQUEST;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.exception.TooManyRequestsException;
import net.chamman.moonnight.rate.limiter.RateLimiter;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter implements RateLimiter {

	private final RedisTemplate<String, String> redisTemplate;

	@Override
	public boolean isAllowed(String key, int maxCount, int timeoutMinutes) {
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		Long reqCount = ops.increment(key, 1);
		log.debug("* Limit 조회. key: [{}], reqCount: [{}], maxCount: [{}], timeoutMinutes: [{}]", key, reqCount, maxCount, timeoutMinutes);

		if (reqCount == 1) {
			redisTemplate.expire(key, timeoutMinutes, TimeUnit.MINUTES);
		}

		if (reqCount > maxCount) {
			String clientIp = CustomRequestContextHolder.getClientIp();
			log.debug("* TooManyRequestsException발생. clientIp: [{}]", clientIp);
			throw new TooManyRequestsException(TOO_MANY_REQUEST, "요청 횟수 초과.");
		}


		return true;
	}

}
