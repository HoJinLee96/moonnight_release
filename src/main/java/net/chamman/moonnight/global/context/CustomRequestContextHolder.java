package net.chamman.moonnight.global.context;

public class CustomRequestContextHolder {

	private static final ThreadLocal<String> clientIpHolder = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> mobileAppHeaderHolder = new ThreadLocal<>();

	public static void setClientIp(String ip) {
		clientIpHolder.set(ip);
	}

	public static String getClientIp() {
		return clientIpHolder.get();
	}

	public static void setMobileApp(boolean mobileApp) {
		mobileAppHeaderHolder.set(mobileApp);
	}

	public static boolean isMobileApp() {
		return mobileAppHeaderHolder.get();
	}

	public static void clear() {
		clientIpHolder.remove();
		mobileAppHeaderHolder.remove();
	}
}
