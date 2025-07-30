package net.chamman.moonnight.global.util;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpExtractor {

	private static final List<String> HEADERS = List.of(
			"X-Forwarded-For",
			"X-Real-IP",
			"Proxy-Client-IP",
			"WL-Proxy-Client-IP",
			"HTTP_CLIENT_IP",
			"HTTP_X_FORWARDED_FOR"
	);

	public static String extractClientIp(HttpServletRequest request) {
		for (String header : HEADERS) {
			String ip = request.getHeader(header);
			if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
				// 다중 IP일 경우 첫 번째 유효한 것만
				String[] ipList = ip.split(",");
				for (String candidate : ipList) {
					String cleaned = candidate.trim();
					if (!cleaned.isEmpty() && !"unknown".equalsIgnoreCase(cleaned)) {
						return normalizeLocalIp(cleaned);
					}
				}
			}
		}
		return normalizeLocalIp(request.getRemoteAddr());
	}

	private static String normalizeLocalIp(String ip) {
		if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
			return "127.0.0.1";
		}
		return ip;
	}
}
