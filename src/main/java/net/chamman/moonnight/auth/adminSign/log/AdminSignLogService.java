package net.chamman.moonnight.auth.adminSign.log;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED_OUT;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.adminSign.log.AdminSignLog.SignResult;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSignLogService {
	
	private final AdminSignLogRepository adminSignLogRepository;
	
	public static List<SignResult> failIncludedResults = List.of(SignResult.INVALID_PASSWORD);
	
	public void registerSignLog(AdminSignLog signLog) {
		try {
			adminSignLogRepository.save(signLog);
		} catch (Exception e) {
			log.error("로그인 로그 기록중 익셉션 발생.",e);
		}
	}
	
	public AdminSignLog signAdmin(Admin admin, SignResult signResult, String clientIp) {
		return adminSignLogRepository.save(
				AdminSignLog.builder()
				.adminId(admin.getAdminId()+"")
				.signResult(signResult)
				.clientIp(clientIp)
				.build());
	}
	
	/** 로그인 실패 횟수 검사
	 * @param adminProvider
	 * @param email
	 * @throws TooManySignFailException {@link AdminSignLogService#validSignFailCount}
	 */
	public int validSignFailCount(String adminId) {
		
		int signFailCount = adminSignLogRepository.countUnresolvedWithResults(adminId, failIncludedResults);
		if (signFailCount >= 10) {
			throw new TooManySignFailException(SIGNIN_FAILED_OUT,"로그인 실패 10회");
		}
		return signFailCount;
	}
	
	/** 비밀번호 변경으로 Sign Fail Count 초기화
	 * @param adminProvider
	 * @param email
	 * @param ip
	 */
	@Transactional
	public void signAdminAndFailLogResolve(Admin admin, SignResult signResult, String clientIp) {
		AdminSignLog adminSignLog = signAdmin(admin, signResult, clientIp);
		adminSignLogRepository.resolveUnresolvedLogs(admin.getAdminId()+"", adminSignLog, failIncludedResults);
	}
	
}
