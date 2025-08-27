package net.chamman.moonnight.global.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.admin.AdminService;
import net.chamman.moonnight.global.annotation.ActiveAdminOnly;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminAuthAspect {
    
    private final AdminService adminService;

    // @AdminOnly 어노테이션이 붙은 메서드 실행 전에 이 코드를 실행
    // 매개변수 중 adminId 값을 통해 엔티티를 조회하여 해당 엔티티의 status를 검사 
    @Before("@annotation(activeAdminOnly)")
    public void verifyAdmin(JoinPoint joinPoint, ActiveAdminOnly activeAdminOnly) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        String[] parameterNames = signature.getParameterNames();
        
        // 3. 메서드로 들어온 실제 값(인자)들을 가져옵니다. (예: [1, Pageable_객체])
        Object[] args = joinPoint.getArgs();
        
        Integer adminId = null;
        
        for (int i = 0; i < parameterNames.length; i++) {
            if ("adminId".equals(parameterNames[i])) {
                if (args[i] instanceof Integer) {
                    adminId = (Integer) args[i];
                    break;
                }
            }
        }
        
        if (adminId == null) {
            throw new ForbiddenException(HttpStatusCode.AUTHORIZATION_FAILED, "@AdminOnly 메서드에 adminId 파라미터가 없거나 타입이 맞지 않습니다.");
        }
        
        adminService.getActiveAdminById(adminId);
    }
}