import {validate, ValidationError} from '/js/validate.js';

/**
 * input 요소에 실시간으로 전화번호 형식(010-1234-5678)을 적용합니다.
 * @param {HTMLInputElement} inputElement - 형식을 적용할 input 요소
 */
export function initPhoneFormatting(inputElement) {
	if (!inputElement) return;

	inputElement.addEventListener('input', (e) => {
		const rawValue = e.target.value.replace(/\D/g, '').substring(0, 12);
		const length = rawValue.length;

		let formattedValue = '';

		// 길이에 따라 하이픈(-)을 추가합니다.
		if (length < 4) {
			formattedValue = rawValue;
		} else if (length < 8) {
			formattedValue = `${rawValue.substring(0, 3)}-${rawValue.substring(3)}`;
		} else if (length < 12) {
			formattedValue = `${rawValue.substring(0, 3)}-${rawValue.substring(3, 7)}-${rawValue.substring(7)}`;
		} else {
			formattedValue = `${rawValue.substring(0, 4)}-${rawValue.substring(4, 8)}-${rawValue.substring(8)}`;
		}

		if (rawValue.startsWith('02')) {
			if (length < 3) {
				formattedValue = rawValue;
			} else if (length < 7) { 
				formattedValue = `${rawValue.substring(0, 2)}-${rawValue.substring(2)}`;
			} else if (length < 10) {
				formattedValue = `${rawValue.substring(0, 2)}-${rawValue.substring(2, 5)}-${rawValue.substring(5)}`;
			} else if (length < 11) {
				formattedValue = `${rawValue.substring(0, 2)}-${rawValue.substring(2, 6)}-${rawValue.substring(6)}`;
			}
		}

		e.target.value = formattedValue;
	});
}

/**
 * input 요소에 실시간으로 생년월일 형식(YYYYMMDD)을 적용합니다.
 * @param {HTMLInputElement} inputElement - 형식을 적용할 input 요소
 */
export function initBirthFormatting(inputElement) {
	if (!inputElement) return;

	inputElement.addEventListener('input', (e) => {
		e.target.value = e.target.value.replace(/\D/g, '').substring(0, 8);
	});
}

/**
 * input 요소에 실시간으로 인증번호 형식(6자리 숫자)을 적용합니다.
 * @param {HTMLInputElement} inputElement - 형식을 적용할 input 요소
 */
export function initVerificationCodeFormatting(inputElement) {
	if (!inputElement) return;

	inputElement.addEventListener('input', (e) => {
		e.target.value = e.target.value.replace(/\D/g, '').substring(0, 6);
	});
}

/**
 * userStatus 값을 한글로 변환하는 헬퍼 함수
 * @param {string} status - e.g., 'ACTIVE', 'SUSPENDED'
 * @returns {string} - e.g., '정상', '정지'
 */
export function formatUserStatus(status) {
	switch (status) {
		case 'ACTIVE':
			return '정상';
		case 'STAY':
			return '일시정지';
		case 'STOP':
			return '정지';
		case 'DELETE':
			return '탈퇴';
		default:
			return status; // 매핑되지 않은 값은 그대로 반환
	}
}

/**
 * ISO 날짜 문자열을 'YYYY년 MM월 DD일' 형식으로 변환하는 헬퍼 함수
 * @param {string} dateString - e.g., '2025-07-22T19:34:26'
 * @returns {string} - e.g., '2025년 07월 22일'
 */
export function formatDate1(dateString) {
	const date = new Date(dateString);
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const day = String(date.getDate()).padStart(2, '0');
	return `${year}년 ${month}월 ${day}일`;
}

/**
 * ISO 날짜 문자열을 'YYYY년 MM월 DD일' 형식으로 변환하는 헬퍼 함수
 * @param {string} dateString - e.g., '2025-07-22T19:34:26'
 * @returns {string} - e.g., '2025년 07월 22일'
 */
export function formatDate2(dateString) {
	if (dateString.length !== 8) {
	    return dateString; // 또는 에러 처리
	}
	try{
		validate('birth',dateString);
	} catch (ValidationError){
		return dateString;
	}
	const year = parseInt(dateString.substring(0, 4), 10);
	const month = parseInt(dateString.substring(4, 6), 10);
	const day = parseInt(dateString.substring(6, 8), 10);
	
	return `${year}년 ${month}월 ${day}일`;
}