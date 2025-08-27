const BASE_URL = '/api/admin/comment';

/**
 * 에러 처리를 위한 헬퍼 함수
 * @param {Response} response - fetch 응답 객체
 * @returns {Promise<any>} - JSON 파싱된 데이터
 */
const handleResponse = async (response) => {
	if (!response.ok) {
		const errorData = await response.json().catch(() => null);
		const errorMessage = errorData?.message || `HTTP error! status: ${response.status}`;
		throw new Error(errorMessage);
	}
	// 댓글 삭제처럼 내용이 없는 성공 응답(204 No Content) 처리
	if (response.status === 204) {
		return null;
	}
	const apiResponse = await response.json();
	console.log(apiResponse);
	return apiResponse.data;
};

/**
 * 특정 견적서의 모든 댓글을 조회하는 API
 * @param {number} estimateId - 견적 ID
 * @returns {Promise<Array>} - 댓글 목록
 */
export const getCommentsByEstimateId = async (estimateId) => {
	const response = await fetch(`${BASE_URL}/private/${estimateId}`);
	return handleResponse(response);
};

/**
 * 새 댓글을 생성하는 API
 * @param {object} commentData - { estimateId, commentText }
 * @returns {Promise<object>} - 생성된 댓글 객체
 */
export const createComment = async (commentData) => {
	const response = await fetch(BASE_URL+'/private/register', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(commentData)
	});
	return handleResponse(response);
};

/**
 * 댓글을 수정하는 API
 * @param {number} commentId - 댓글 ID
 * @param {string} commentText - 수정할 댓글 내용
 * @returns {Promise<object>} - 수정된 댓글 객체
 */
export const updateComment = async (commentId, newText, version) => {
	const response = await fetch(`${BASE_URL}/private/${commentId}`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ comment: newText, version: version })
	});
	return handleResponse(response);
};

/**
 * 댓글을 삭제하는 API
 * @param {number} commentId - 댓글 ID
 * @returns {Promise<null>}
 */
export const deleteComment = async (commentId, version) => {
	
	const response = await fetch(`${BASE_URL}/private/${commentId}`, {
		method: 'DELETE',
		body: new URLSearchParams({ version })
	});
	return handleResponse(response);
};
