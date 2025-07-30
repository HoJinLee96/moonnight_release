package net.chamman.moonnight.auth.oauth;

import static net.chamman.moonnight.global.exception.HttpStatusCode.OAUTH_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.OAUTH_STATUS_STAY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.OAUTH_STATUS_STOP;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.auth.sign.log.SignLog;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusStayException;
import net.chamman.moonnight.global.exception.status.StatusStopException;

@Service
@RequiredArgsConstructor
public class OAuthService {

	private final OAuthRepository oauthRepository;
	private final SignLogService signLogService;
	private final Obfuscator obfuscator;

	public Optional<OAuth> getOAuth(OAuthProvider oauthProvider, String oauthProviderId) {

		Optional<OAuth> oauth = oauthRepository.findByOauthProviderAndOauthProviderId(oauthProvider, oauthProviderId);
		if(oauth.isPresent()) {
			validateOAuth(oauth.get());
		}
		return oauth;
	}
	
	public OAuth getOAuthByOAuthId(int oauthId) {
		OAuth oauth = oauthRepository.findByOauthId(oauthId).orElseThrow(() -> new NoSuchDataException(OAUTH_NOT_FOUND, "찾을 수 없는 유저."));;
		validateOAuth(oauth);
		return oauth;
	}

	public List<OAuthResponseDto> getOAuthByUser(User user) {
		List<OAuth> existingOAuths = oauthRepository.findByUser(user);
		List<OAuthResponseDto> oauthResponseList = new ArrayList<>();
		for (OAuth oauth : existingOAuths) {
			oauthResponseList.add(OAuthResponseDto.fromEntity(oauth, obfuscator));
		}
		return oauthResponseList;
	}

	public void deleteOAuthByUserId(int userId, String clientIp) {
		List<OAuth> list = oauthRepository.findByUserId(userId);
		for (OAuth oauth : list) {
			deleteOAuth(oauth.getOauthId(), clientIp);
		}
	}

	@Transactional
	public void deleteOAuth(int oauthId, String clientIp) {
		OAuth oauth = oauthRepository.findById(oauthId).orElseThrow(() -> new NoSuchDataException(OAUTH_NOT_FOUND));
		validateOAuth(oauth);
		oauthRepository.deleteById(oauthId);
		signLogService.registerSignLog(SignLog.builder()
				.provider(oauth.getOauthProvider().name())
				.id(oauthId + "")
				.clientIp(clientIp)
				.signResult(SignResult.UNLINK)
				.build());
	}

	@SuppressWarnings("incomplete-switch")
	public static void validateOAuth(OAuth oauth) {
		switch (oauth.getOauthStatus()) {
		case STAY: {
			throw new StatusStayException(OAUTH_STATUS_STAY);
		}
		case STOP: {
			throw new StatusStopException(OAUTH_STATUS_STOP);
		}
		}

	}
}
