package net.chamman.moonnight.infra.map.dto;

import java.util.List;

public abstract class RoadSearchApiResponseDto {
    public abstract List<StandardAddress> getStandardAddresses();
	public abstract boolean isEmpty();
	public abstract boolean isEnd();
}
