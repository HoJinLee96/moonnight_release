package net.chamman.moonnight.global.util;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.SerializationUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

	public static void addCookie(HttpServletResponse res, String name, String value, Duration duration) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(duration)
				.sameSite("Lax")
				.build();
		
		res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }
    
    public static String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

	public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                        Base64.getUrlDecoder().decode(cookie.getValue())));
    }
//    public static String serialize(Object object) {
//        try {
//            String json = objectMapper.writeValueAsString(object);
//            return Base64.getUrlEncoder().encodeToString(json.getBytes());
//        } catch (Exception e) {
//            throw new EncodingException(ENCODING_FAIL, "객체 직렬화 실패.", e);
//        }
//    }
//
//    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
//        try {
//            byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
//            return objectMapper.readValue(new String(decodedBytes), cls);
//        } catch (Exception e) {
//            throw new DecodingException(DECODING_FAIL, "쿠키 역직렬화 실패.", e);
//        }
//    }
}