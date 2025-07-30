import { validate, ValidationError } from '/js/validate.js';

/**
 * 견적서 FormData의 유효성을 검사하는 비동기 함수
 * @param {FormData} formData - 서버로 보낼 FormData 객체
 */
async function validateFormData(formData) {
	// 1. FormData에서 DTO와 파일들을 추출
	const dtoBlob = formData.get('estimateRequestDto'); // 백엔드와 맞춘 DTO 키
	const imageFiles = formData.getAll('images');      // 백엔드와 맞춘 파일 키

	if (!dtoBlob) {
		throw new ValidationError('견적 정보가 누락되었습니다.');
	}

	// 2. Blob을 JavaScript 객체로 변환
	const dto = JSON.parse(await dtoBlob.text());

	// 3. validate.js의 validate 함수를 사용하여 각 필드 검증
	validate('name', dto.name);
	
	if (!dto.phoneAgree && !dto.emailAgree) {
		throw new ValidationError('최소 하나 이상의 수신 방법을 인증해주세요.');
	}

	// 동의한 항목에 대해서만 유효성 검사 실행
	if (dto.phoneAgree) {
		validate('phone', dto.phone);
	}
	if (dto.emailAgree) {
		validate('email', dto.email);
	}
	
	// 주소 유효성 검사
	validate('address', dto.mainAddress);
    validate('address', dto.detailAddress);


	// 4. 이미지 개수 검증
	// (수정의 경우) DTO에 포함된 기존 이미지 경로와 새로 첨부된 파일 수를 합산
	const totalImageCount = (dto.imagesPath ? dto.imagesPath.length : 0) + imageFiles.length;
	if (totalImageCount > 10) {
		throw new ValidationError('이미지는 최대 10장까지 업로드할 수 있습니다.');
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
 * 나의 모든 견적서 목록을 가져옵니다.
 * @returns {Promise<object>} 견적서 목록 데이터
 */
export async function getMyAllEstimate() {
	const response = await fetch("/api/estimate/private/user", {
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
 * 나의 특정 견적서를 조회합니다.
* @param {number} estimateId - 조회할 견적서의 ID
 */
export async function getEstimate(estimateId) {
	const response = await fetch(`/api/estimate/private/user/${estimateId}`, {
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
 * AUTH TOKEN 통해 견적서 목록을 가져옵니다.
 * @returns {Promise<object>} 견적서 목록 데이터
 */
export async function getAllEstimateByAuth() {
	const response = await fetch("/api/estimate/private/auth", {
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
		headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
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
 * 로그인 유저 견적서를 수정 합니다.
 */
export async function updateEstimateByUser(estimateId, formData) {
	
	await validateFormData(formData);

	const response = await fetch(`/api/estimate/private/update/${estimateId}`, {
		method: "PATCH", 
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
 * AUTH TOKEN 통해 견적서를 수정 합니다.
 * @param {string} estimateId - 수정할 견적서의 ID
 * @param {FormData} formData - imageHandler가 만들어준 완성된 FormData 객체
 */
export async function updateEstimateByAuth(estimateId, formData) {
	
	await validateFormData(formData);

    const response = await fetch(`/api/estimate/private/auth/update/${estimateId}`, {
        method: "PATCH", // 또는 PUT
        headers: {},
        body: formData // 전달받은 formData를 그대로 body에 넣는다.
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
 * 로그인 유저 견적서를 삭제합니다.
 * @param {number} estimateId - 삭제할 견적서의 ID
 */
export async function deleteEstimateByUser(estimateId) {
	const response = await fetch(`/api/estimate/private/${estimateId}`, {
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
 * AUTH TOKEN 통해 견적서를 삭제합니다.
 * @param {number} estimateId - 삭제할 견적서의 ID
 */
export async function deleteEstimateByAuth(estimateId) {
	const response = await fetch(`/api/estimate/private/auth/${estimateId}`, {
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