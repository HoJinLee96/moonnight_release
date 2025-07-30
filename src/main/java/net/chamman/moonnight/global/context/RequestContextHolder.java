package net.chamman.moonnight.global.context;

import org.springframework.security.core.userdetails.UserDetails;

public class RequestContextHolder <T extends UserDetails> {

    private static final ThreadLocal<RequestContext> contextHolder = new ThreadLocal<>();

    public static void setContext(RequestContext context) {
        contextHolder.set(context);
    }

    public static RequestContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
    
}
