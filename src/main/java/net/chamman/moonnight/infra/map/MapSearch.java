package net.chamman.moonnight.infra.map;

import net.chamman.moonnight.infra.map.dto.RoadSearchApiResponseDto;

public interface MapSearch {
	RoadSearchApiResponseDto roadSearch(String road, String analyzeType, int page, int size);
}
