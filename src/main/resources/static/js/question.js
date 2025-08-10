/**
 * question.js
 * 질문 게시판 관련 C/R/U/D 및 기타 기능에 대한 fetch API 모듈
 */

const BASE_URL = '/api/question';

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
 * 질문 등록
 */
export const registerQuestion = (questionData) => {
    return fetchApi(`${BASE_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(questionData),
    });
};

/**
 * 질문 조회를 위한 비밀번호 확인
 */
export const verifyPassword = (questionId, password) => {
    return fetchApi(`${BASE_URL}/${questionId}/verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
    });
};

/**
 * 질문 수정
 * @param {number} questionId
 * @param {object} questionData - { title, content, password, version }
 */
export const modifyQuestion = (questionId, questionData) => {
    return fetchApi(`${BASE_URL}/${questionId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(questionData),
    });
};

/**
 * 질문 삭제
 * @param {number} questionId
 * @param {string} password
 * @param {number} version
 */
export const deleteQuestion = (questionId, password, version) => {
    return fetchApi(`${BASE_URL}/${questionId}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: password, version: version }),
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
