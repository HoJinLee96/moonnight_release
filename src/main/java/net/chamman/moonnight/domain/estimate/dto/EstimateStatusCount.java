package net.chamman.moonnight.domain.estimate.dto;

public record EstimateStatusCount(
		long all,
		long receive,
		long inProgress,
		long complete,
		long delete
		) {

}
