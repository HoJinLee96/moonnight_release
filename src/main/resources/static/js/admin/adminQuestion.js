const BASE_URL = '/api/admin/question';

// API 요청을 위한 공통 fetch 함수
const fetchApi = async (url, options) => {
	try {
	    const response = await fetch(url, options);
	    const result = await response.json();

	    if (!response.ok) {
	        // API가 반환하는 에러 메시지를 우선적으로 사용
	        throw new Error(result.message || `HTTP error! status: ${response.status}`);
	    }
	    return result.data; // 성공 시 'data' 필드 반환
	} catch (error) {
	    // 사용자에게 에러 메시지 표시
	    alert(error.message);
	    throw error; // 에러를 다시 던져서 호출한 쪽에서 추가 처리할 수 있도록 함
	}
};

/**
 * (관리자) 질문 내용을 수정합니다.
 * @param {number} questionId - 수정할 질문의 ID
 * @param {object} modifyData - { title, content, version }
 * @returns {Promise<object>} 수정된 질문 데이터 (QuestionResponseDto)
 */
export const modifyQuestionByAdmin = (questionId, modifyData) => {
    return fetchApi(`${BASE_URL}/${questionId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(modifyData),
    });
};

/**
 * (관리자) 질문의 상태를 변경합니다.
 * @param {number} questionId - 상태를 변경할 질문의 ID
 * @param {object} statusData - { questionStatus, version }
 * @returns {Promise<object>} 상태가 변경된 질문 데이터 (QuestionResponseDto)
 */
export const updateQuestionStatusByAdmin = (questionId, statusData) => {
    return fetchApi(`${BASE_URL}/${questionId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(statusData),
    });
};

/**
 * (관리자) 질문을 삭제합니다.
 * @param {number} questionId - 삭제할 질문의 ID
 * @param {object} deleteData - { version }
 * @returns {Promise<object>} 삭제된 질문 데이터 (QuestionResponseDto)
 */
export const deleteQuestionByAdmin = (questionId, deleteData) => {
    // AdminQuestionDeleteRequestDto에는 version만 필요합니다.
    return fetchApi(`${BASE_URL}/${questionId}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(deleteData),
    });
};

// --- 목록 조회 관련 함수들 ---
export const fetchQuestions = (page = 0, size = 10) => {
    const url = `${BASE_URL}?page=${page}&size=${size}&sort=createdAt,desc`;
    return fetchApi(url, { method: 'GET' });
};

export const searchQuestions = (title, page = 0, size = 10) => {
    const url = `${BASE_URL}/search?title=${encodeURIComponent(title)}&page=${page}&size=${size}&sort=createdAt,desc`;
    return fetchApi(url, { method: 'GET' });
};

// --- 단일 조회 관련 함수 ---
export const getQuestions = (questionId) => {
    const url = `${BASE_URL}/${questionId}`;
    return fetchApi(url, { method: 'GET' });
};

