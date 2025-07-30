package net.chamman.moonnight.auth.sign.log;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED_OUT;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignLogService {
	
	private final SignLogRepository signLogRepository;
	
	public static List<SignResult> failIncludedResults = List.of(SignResult.INVALID_PASSWORD);
	
	public void registerSignLog(SignLog signLog) {
		try {
			signLogRepository.save(signLog);
		} catch (Exception e) {
			log.error("로그인 로그 기록중 익셉션 발생.",e);
		}
	}
	
	public SignLog signUser(User user, SignResult signResult, String clientIp) {
		return signLogRepository.save(
				SignLog.builder()
				.provider(user.getUserProvider().name())
				.id(user.getUserId()+"")
				.email(user.getEmail())
				.signResult(signResult)
				.clientIp(clientIp)
				.build());
	}
	
	/** 로그인 실패 횟수 검사
	 * @param userProvider
	 * @param email
	 * @throws TooManySignFailException {@link SignLogService#validSignFailCount}
	 */
	public int validSignFailCount(UserProvider userProvider, String id) {
		
		int signFailCount = signLogRepository.countUnresolvedWithResults(userProvider.name(), id, failIncludedResults);
		if (signFailCount >= 10) {
			throw new TooManySignFailException(SIGNIN_FAILED_OUT,"로그인 실패 10회");
		}
		return signFailCount;
	}
	
	/** 비밀번호 변경으로 Sign Fail Count 초기화
	 * @param userProvider
	 * @param email
	 * @param ip
	 */
	@Transactional
	public void signUserAndFailLogResolve(User user, SignResult signResult, String clientIp) {
		SignLog signLog = signUser(user, signResult, clientIp);
		signLogRepository.resolveUnresolvedLogs(user.getUserId()+"", signLog, failIncludedResults);
	}
	
}
