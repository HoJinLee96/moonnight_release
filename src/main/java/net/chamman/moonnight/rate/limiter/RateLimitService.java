package net.chamman.moonnight.rate.limiter;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimiter rateLimiter;

    public void checkVerificationCodeRequest(String recipient) {
    	log.debug("* 인증 코드 요청 횟수 체크: [{}]", LogMaskingUtil.maskRecipient(recipient, MaskLevel.MEDIUM));
    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.VERIFICATION_CODE_REQUEST.key(recipient),
            RateLimitKeyGenerator.VERIFICATION_CODE_REQUEST.getMaxRequest(),
            RateLimitKeyGenerator.VERIFICATION_CODE_REQUEST.getTimeoutMinutes()
        );
    }

    public void checkEstimateRegister(String clientIp) {
    	log.debug("* 견적서 등록 요청 횟수 체크 clientIp: [{}]",LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM));

    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.ESTIMATE_REGISTER.key(clientIp),
            RateLimitKeyGenerator.ESTIMATE_REGISTER.getMaxRequest(),
            RateLimitKeyGenerator.ESTIMATE_REGISTER.getTimeoutMinutes()
        );
    }
    
    public void checkQuestionRegister(String clientIp) {
    	log.debug("* 질문 등록 요청 횟수 체크 clientIp: [{}]",LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM));

    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.QUESTION_REGISTER.key(clientIp),
            RateLimitKeyGenerator.QUESTION_REGISTER.getMaxRequest(),
            RateLimitKeyGenerator.QUESTION_REGISTER.getTimeoutMinutes()
        );
    }
    
    public void checkRequestClientIp(String clientIp) {
    	log.debug("* 요청 횟수 체크 clientIp: [{}]",LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM));

    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.REQUEST_CLIENT_IP.key(clientIp),
            RateLimitKeyGenerator.REQUEST_CLIENT_IP.getMaxRequest(),
            RateLimitKeyGenerator.REQUEST_CLIENT_IP.getTimeoutMinutes()
        );
    }
}
