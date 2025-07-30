import { validate } from '/js/validate.js';


// ======= 휴대폰 인증번호 발송 =======
export async function sendSms(phone) {

	validate('phone', phone);

	const response = await fetch("/api/verify/public/sms", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ phone })
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

// ======= 휴대폰 인증번호 검증 =======
export async function verifySmsCode(phone, code) {

	validate('phone', phone);
	validate('code', code);

	const body = {
		phone: phone,
		verificationCode: code
	};

	const response = await fetch("/api/verify/public/compare/sms/uuid", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(body)
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