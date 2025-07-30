/**
 * 견적서 폼의 유효성을 검사하는 함수
 * @param {object} simpleEstimateDto - 간편 견적서 데이터 객체
 */
function validateSimpleEstimate(simpleEstimateDto) {
	if (simpleEstimateDto.phone == "") {
		const error = new Error('연락처를 입력해 주세요.'); 
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
    } else 	if (simpleEstimateDto.cleaningService == "") {
		const error = new Error('청소 서비스를 선택해 주세요.'); 
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	} else 	if (simpleEstimateDto.region == "") {
		const error = new Error('지역을 선택해 주세요.'); 
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
}

/**
 * 간편 견적서를 서버에 등록합니다.
 * @param {object} simpleEstimateDto - 간편 견적서의 텍스트 데이터
 */
export async function registerSimpleEstimate(simpleEstimateDto) {
	
	validateSimpleEstimate(simpleEstimateDto);

	const response = await fetch("/api/spem/public/register", {
		method: "POST",
		headers: {"Content-Type": "application/json"},
		body: JSON.stringify(simpleEstimateDto)
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.'); 
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 나의 모든 견적서 목록을 가져옵니다.
 * @returns {Promise<object>} 견적서 목록 데이터
 */
export async function getMyAllSimpleEstimate() {
	const response = await fetch("/api/spem/private/user", {
		method: "GET"
	});
	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.'); 
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 특정 견적서를 조회합니다.
* @param {number} simpleEstimateId - 조회할 간편 견적서의 ID
 */
export async function getMySimpleEstimate(simpleEstimateId) {
	const response = await fetch(`/api/spem/private/user/${simpleEstimateId}`, {
		method: "GET"
	});
	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.'); 
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 특정 간편 견적서를 삭제합니다.
 * @param {number} simpleEstimateId - 삭제할 견적서의 ID
 */
export async function deleteMySimpleEstimate(simpleEstimateId) {
	const response = await fetch(`/api/estimate/private/${simpleEstimateId}`, {
		method: "DELETE", 
	});

	if (!response.ok) {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.'); 
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 휴대폰 인증 로그인 권한으로 특정 간편 견적서를 삭제합니다.
 * @param {number} simpleEstimateId - 삭제할 견적서의 ID
 */
export async function deleteSimpleEstimateByAuth(simpleEstimateId) {
	const response = await fetch(`/api/estimate/private/auth/${simpleEstimateId}`, {
		method: "DELETE", 
	});

	if (!response.ok) {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.'); 
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

