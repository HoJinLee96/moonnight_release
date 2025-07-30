import { validate, ValidationError } from '/js/validate.js';

export async function getAccessToUpdatePasswordByPhone(email, phone) {
	validate("email", email);
	validate("phone", phone);

	const response = await fetch("/api/user/public/find/pw/by/phone", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email, phone })
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		let error;
		if (json.code === "4520") {
			error = new Error('비밀번호가 일치하지 않습니다.');
		} else {
			error = new Error(json.message || '서버 요청에 실패했습니다.');
		}
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

export async function getAccessToUpdatePasswordByEmail(email) {
	validate("email", email);

	const response = await fetch("/api/user/public/find/pw/by/email", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		let error;
		if (json.code === "4520") {
			error = new Error('비밀번호가 일치하지 않습니다.');
		} else {
			error = new Error(json.message || '서버 요청에 실패했습니다.');
		}
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

export async function updatePassword(password, confirmPassword) {
	validate('password', password);
	validate('password', confirmPassword);

	if (password !== confirmPassword) {
		throw new ValidationError("두 비밀번호가 일치하지 않습니다.");
	}

	const response = await fetch("/api/user/public/update/pw", {
		method: "PATCH",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ password, confirmPassword })
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		let error;
		if (json.code === "4520") {
			error = new Error('비밀번호가 일치하지 않습니다.');
		} else {
			error = new Error(json.message || '서버 요청에 실패했습니다.');
		}
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

export async function updateProfile(profileData) {
	validate('name', profileData.name);
	validate('birth', profileData.birth);

	const response = await fetch("/api/user/private/update/profile", {
		method: "PATCH",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(profileData)
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

export async function updatePhone(phone) {
	validate('phone', phone);

	const response = await fetch("/api/user/private/update/phone", {
		method: "PATCH",
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

// ======= 이메일 유효성 + 중복 검사 =======
export async function isEmailExist(email) {

	validate('email', email);

	const response = await fetch("/api/user/public/exist/email", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
	});

	if (response.ok) {
	} else {
		const json = await response.json();
		if (json.code == '4531') {
			const error = new Error('이미 가입되어 있는 이메일 입니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		} else {
			const error = new Error(json.message || '서버 요청에 실패했습니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		}
	}
}

export async function isEmailExistForFindEmail(phone) {

	const response = await fetch("/api/user/public/find/email/by/phone", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ phone })
	});
	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		let error;
		if (json.code === "4530") {
			error = new Error('해당 사용자를 찾을 수 없습니다.');
		} else {
			error = new Error(json.message || '서버 요청에 실패했습니다.');
		}
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

export async function isEmailExistForConvertToLocal(email) {

	validate('email', email);

	const response = await fetch("/api/user/private/exist/email/convert", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
	});

	if (response.ok) {
	} else {
		const json = await response.json();
		if (json.code == '4531') {
			const error = new Error('이미 가입되어 있는 이메일 입니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		} else {
			const error = new Error(json.message || '서버 요청에 실패했습니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		}
	}
}

export async function isEmailExistsForFindPassword(email) {

	validate('email', email);

	const response = await fetch("/api/user/public/exist/email/find/password", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
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

export async function isPhoneExist(phone) {

	validate('phone', phone);

	const response = await fetch("/api/user/public/exist/phone", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ phone })
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		if (json.code == '4532') {
			const error = new Error('이미 가입되어 있는 휴대폰 입니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		} else {
			const error = new Error(json.message || '서버 요청에 실패했습니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		}
	}
}