import { validateFormData } from '/js/estimate.js';


const BASE_URL = '/api/admin/estimate/private';

/**
 * 에러 처리를 위한 헬퍼 함수
 * @param {Response} response - fetch 응답 객체
 * @returns {Promise<any>} - JSON 파싱된 데이터
 */
const handleResponse = async (response) => {
	if (!response.ok) {
		// 서버에서 보낸 에러 메시지를 우선적으로 사용
		const errorData = await response.json().catch(() => null);
		const errorMessage = errorData?.message || `HTTP error! status: ${response.status}`;
		throw new Error(errorMessage);
	}
	const apiResponse = await response.json();
	if (apiResponse.status !== 200) {
		throw new Error(apiResponse.message || 'API 응답 상태가 정상이 아닙니다.');
	}
	return apiResponse.data;
};

/**
 * 견적 목록을 검색하는 API
 * @param {object} params - { estimateSearchRequestDto, page, size }
 * @returns {Promise<object>} - 성공 시 검색 결과 데이터, 실패 시 에러 throw
 */
export const searchEstimates = async ({ estimateSearchRequestDto, page, size }) => {
	const response = await fetch(`${BASE_URL}/search?page=${page}&size=${size}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(estimateSearchRequestDto)
	});
	return handleResponse(response);
};

/**
 * 특정 ID의 견적 상세 정보를 조회하는 API
 * @param {number} estimateId - 견적 ID
 * @returns {Promise<object>} - 성공 시 견적 상세 데이터, 실패 시 에러 throw
 */
export const getEstimateById = async (estimateId) => {
	// TODO: 이 API를 처리할 백엔드 컨트롤러 메서드가 필요함
	// 예: @GetMapping("/private/{id}")
	const response = await fetch(`${BASE_URL}/${estimateId}`);
	return handleResponse(response);
};

export const updateEstimate = async (estimateId, formData) => {
	await validateFormData(formData);
	const response = await fetch(`${BASE_URL}/update/${estimateId}`, {
		method: 'PATCH',
		body: formData
	});
	return handleResponse(response);
};

export const updateEstimateStatus = async (data) => {
	const response = await fetch(`${BASE_URL}/update/status}`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(data)
	});
	return handleResponse(response);
};

/**
 * 여러 견적서의 상태를 일괄 변경하는 API
 * @param {number[]} estimateIds - 변경할 견적서 ID 배열
 * @param {string} targetStatus - 목표 상태 (예: 'IN_PROGRESS')
 * @returns {Promise<any>}
 */
export const updateMultipleEstimateStatus = async (data) => {
	const response = await fetch(`${BASE_URL}/update/status/bulk`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(data)
	});
	return handleResponse(response);
};

export const deleteEstimate = async (estimateId, version) => {
	const response = await fetch(`${BASE_URL}/${estimateId}`, {
		method: "DELETE",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ version })
	});

	return handleResponse(response);
}