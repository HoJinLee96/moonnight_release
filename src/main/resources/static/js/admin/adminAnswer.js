const BASE_URL = '/api/admin/answer';

// API 요청을 위한 공통 fetch 함수
const fetchApi = async (url, options) => {
	try {
	    const response = await fetch(url, options);

	    if (!response.ok) {
	        let errorData;
	        try {
	            errorData = await response.json();
	        } catch (e) {
	            throw new Error(`HTTP error! status: ${response.status}`);
	        }
	        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
	    }

	    if (response.status === 204) {
	        return; // No content to parse
	    }
	    
	    const result = await response.json();
	    return result.data; // Success, return 'data' field
	} catch (error) {
	    alert(error.message);
	    throw error;
	}
};

/**
 * (관리자) 새로운 답변을 등록합니다.
 * @param {object} createData - { questionId, content }
 * @returns {Promise<object>} 등록된 답변 데이터 (AdminAnswerResponseDto)
 */
export const registerAnswerByAdmin = (createData) => {
    return fetchApi(`${BASE_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(createData),
    });
};

/**
 * (관리자) 기존 답변을 수정합니다.
 * @param {number} answerId - 수정할 답변의 ID
 * @param {object} modifyData - { content, version }
 * @returns {Promise<object>} 수정된 답변 데이터 (AdminAnswerResponseDto)
 */
export const modifyAnswerByAdmin = (answerId, modifyData) => {
    return fetchApi(`${BASE_URL}/${answerId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(modifyData),
    });
};

/**
 * (관리자) 답변을 삭제합니다.
 * @param {number} answerId - 삭제할 답변의 ID
 * @param {object} deleteData - { version }
 * @returns {Promise<void>}
 */
export const deleteAnswerByAdmin = (answerId, deleteData) => {
    return fetchApi(`${BASE_URL}/${answerId}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(deleteData),
    });
};
