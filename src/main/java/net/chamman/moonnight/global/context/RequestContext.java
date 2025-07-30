package net.chamman.moonnight.global.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestContext {

	private final String clientIp;
    private boolean isMobileApp;

}
