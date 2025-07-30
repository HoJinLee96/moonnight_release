package net.chamman.moonnight.infra.naver;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public class NaverSignatureGenerator {
	
	@Value("${naver-api.accessKey}")
	private String accessKey;
	
	@Value("${naver-api.secretKey}")
	private String secretKey;
	
	public String getNaverSignature(String method, String url, String time) throws UnsupportedEncodingException, NoSuchAlgorithmException, java.security.InvalidKeyException {
		
		String message = new StringBuilder()
				.append(method)
				.append(" ")
				.append(url)
				.append("\n")
				.append(time)
				.append("\n")
				.append(accessKey)
				.toString();
		
		SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(signingKey);
		
		byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
		String encodeBase64String = Base64.getEncoder().encodeToString(rawHmac);
		
		return encodeBase64String;
	}
	
}
