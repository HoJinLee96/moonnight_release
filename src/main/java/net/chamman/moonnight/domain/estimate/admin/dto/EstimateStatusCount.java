package net.chamman.moonnight.domain.estimate.admin.dto;

public record EstimateStatusCount(
		long all,
		long receive,
		long inProgress,
		long complete,
		long delete
		) {

}
