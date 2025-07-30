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

    public void checkPhoneVerify(String phone) {
    	log.debug("* RateLimitService.checkPhoneVerify() phone: [{}]",LogMaskingUtil.maskPhone(phone, MaskLevel.MEDIUM));
    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.VERIFY_PHONE.key(phone),
            RateLimitKeyGenerator.VERIFY_PHONE.getMaxRequest()
        );
    }

    public void checkEmailVerify(String email) {
    	log.debug("* RateLimitService.checkEmailVerify() email: [{}]",LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM));

    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.VERIFY_EMAIL.key(email),
            RateLimitKeyGenerator.VERIFY_EMAIL.getMaxRequest()
        );
    }

    public void checkEstimateByIp(String clientIp) {
    	log.debug("* RateLimitService.checkEstimateByIp() clientIp: [{}]",LogMaskingUtil.maskIp(clientIp, MaskLevel.MEDIUM));

    	rateLimiter.isAllowed(
            RateLimitKeyGenerator.ESTIMATE.key(clientIp),
            RateLimitKeyGenerator.ESTIMATE.getMaxRequest()
        );
    }
}
