import { validate, ValidationError } from '/js/validate.js';

/**
 * 견적서 FormData의 유효성을 검사하는 비동기 함수
 * @param {FormData} formData - 서버로 보낼 FormData 객체
 */
export async function validateFormData(formData) {
	// 1. FormData에서 DTO와 파일들을 추출
	const dtoBlob = formData.get('dto'); // 백엔드와 맞춘 DTO 키
	const imageFiles = formData.getAll('images');      // 백엔드와 맞춘 파일 키

	if (!dtoBlob) {
		throw new ValidationError('견적서 정보가 누락되었습니다.');
	}

	// 2. Blob을 JavaScript 객체로 변환
	const dto = JSON.parse(await dtoBlob.text());

	// 3. validate.js의 validate 함수를 사용하여 각 필드 검증
	validate('estimateName', dto.name);

	// 주소 유효성 검사
	validate('address', dto.mainAddress);

	// 4. 이미지 개수 검증
	// (수정의 경우) DTO에 포함된 기존 이미지 경로와 새로 첨부된 파일 수를 합산
	const totalImageCount = (dto.imagesPath ? dto.imagesPath.length : 0) + imageFiles.length;
	if (totalImageCount > 10) {
		throw new ValidationError('이미지는 최대 10장까지 업로드할 수 있습니다.');
	}

}

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
	} else if (simpleEstimateDto.cleaningService == "") {
		const error = new Error('청소 서비스를 선택해 주세요.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	} else if (simpleEstimateDto.region == "") {
		const error = new Error('지역을 선택해 주세요.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
}

/**
 * 견적서를 서버에 등록합니다. (파일 포함)
 * @param {object} estimateDto - 견적서의 텍스트 데이터 (name, phone, content 등)
 * @param {File[]} imageFiles - 사용자가 선택한 이미지 파일의 배열
 * @returns {Promise<object>} 성공 시 서버로부터 받은 데이터
 */
export async function registerEstimate(formData) {

	await validateFormData(formData);

	const response = await fetch("/api/estimate/public/register", {
		method: "POST",
		body: formData
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
 * 간편 견적서를 서버에 등록합니다.
 * @param {object} simpleEstimateDto - 간편 견적서의 텍스트 데이터
 */
export async function registerSimpleEstimate(simpleEstimateDto) {

	validateSimpleEstimate(simpleEstimateDto);

	const response = await fetch("/api/estimate/public/register/simple", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
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
 * AUTH TOKEN 통해 견적서 목록을 가져옵니다.
 * @returns {Promise<object>} 견적서 목록 데이터
 */
export async function getAllEstimateByAuth(page = 0, size = 20) {
	const response = await fetch(`/api/estimate/private/auth?page=${page}&size=${size}`, {
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
 * AUTH TOKEN 통해 특정 견적서를 조회합니다.
* @param {number} estimateId - 조회할 견적서의 ID
 */
export async function getEstimateByAuth(estimateId) {
	const response = await fetch(`/api/estimate/private/auth/${estimateId}`, {
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
 * 견적 번호와 인증 정보통해 견적서를 조회합니다.
 */
export async function getEstimateByGuest(estimateId, recipient) {
	const body = new URLSearchParams({
		estimateId,
		recipient
	});

	const response = await fetch('/api/estimate/public/guest', {
		method: "POST",
		headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
		body: body
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
 * AUTH TOKEN 통해 견적서를 수정 합니다.
 * @param {string} estimateId - 수정할 견적서의 ID
 * @param {FormData} formData - imageHandler가 만들어준 완성된 FormData 객체
 */
export async function updateEstimateByAuth(estimateId, formData) {

	await validateFormData(formData);

	const response = await fetch(`/api/estimate/private/auth/update/${estimateId}`, {
		method: "PATCH",
		headers: {},
		body: formData
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
 * AUTH TOKEN 통해 견적서의 휴대폰 인증을 해제 합니다.
 * @param {string} estimateId - 수정할 견적서의 ID
 * @param {number} version
 */
export async function clearEstimatePhone(estimateId, version) {

	const response = await fetch(`/api/estimate/private/auth/clear/phone/${estimateId}`, {
		method: "PATCH",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ version })
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
 * AUTH TOKEN과 VerificationPhoneToken 통해 견적서의 휴대폰을 변경 합니다.
 * @param {string} estimateId - 수정할 견적서의 ID
 * @param {number} version
 */
export async function updateEstimatePhoneByVerification(estimateId, version) {

	const response = await fetch(`/api/estimate/private/auth/update/phone/${estimateId}`, {
		method: "PATCH",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ version })
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
 * AUTH TOKEN 통해 견적서의 이메일 인증을 해제 합니다.
 * @param {string} estimateId - 수정할 견적서의 ID
 * @param {number} version
 */
export async function clearEstimateEmail(estimateId, version) {

	const response = await fetch(`/api/estimate/private/auth/clear/email/${estimateId}`, {
		method: "PATCH",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ version })
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
 * AUTH TOKEN과 VerificationEmailToken 통해 견적서의 휴대폰을 변경 합니다.
 * @param {string} estimateId - 수정할 견적서의 ID
 * @param {number} version
 */
export async function updateEstimateEmailByVerification(estimateId, version) {

	const response = await fetch(`/api/estimate/private/auth/update/email/${estimateId}`, {
		method: "PATCH",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ version })
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
 * AUTH TOKEN 통해 견적서를 삭제합니다.
 * @param {number} estimateId - 삭제할 견적서의 ID
 * @param {number} version
 */
export async function deleteEstimateByAuth(estimateId, version) {
	const response = await fetch(`/api/estimate/private/auth/${estimateId}`, {
		method: "DELETE",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ version })
	});

	if (!response.ok) {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}