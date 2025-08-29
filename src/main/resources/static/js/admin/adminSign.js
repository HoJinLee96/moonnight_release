import { validate, ValidationError } from '/js/validate.js';

/**
 * 로그인
 */
export async function signIn(signInDto, rememberEmail) {

	validate('email', signInDto.email);
	validate('password', signInDto.password);

	let apiUrl = "/api/admin/public/signIn";

	const response = await fetch(apiUrl, {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(signInDto)
	});

	if (response.ok) {
		const json = await response.json();
		if (rememberEmail) {
			localStorage.setItem("rememberEmail", signInDto.email);
		} else {
			localStorage.removeItem("rememberEmail");
		}
		window.location.href = "/admin/home";
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 로그아웃
 */
export async function signOut() {
	const response = await fetch("/api/admin/private/signOut", {
		method: "POST"
	});
	if (response.ok) {
		return;
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 관리자 회원가입 이메일 중복 검사
 */
export async function isEmailExistForSignUp(email) {

	validate('email', email);

	const response = await fetch("/api/admin/public/signUp/exist/email", {
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

/**
 * 관리자 회원가입 1차
 */
export async function signUpStep1(email, password, confirmPassword) {

	validate('email', email);
	validate('password', password);
	if (password !== confirmPassword) {
		throw new ValidationError('두 비밀번호가 일치하지 않습니다.');
	}

	const response = await fetch("/api/admin/public/signUp/first", {
		method: "POST",
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
		},
		body: new URLSearchParams({ email, password, confirmPassword })
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
 * 관리자 회원가입 휴대폰 중복 검사
 */
export async function isPhoneExistForSignUp(phone) {

	validate('phone', phone);

	const response = await fetch("/api/admin/public/signUp/exist/phone", {
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

/**
 * 관리자 회원가입 2차
 */
export async function signUpStep2(signUpRequestDto) {

	validate('name', signUpRequestDto.name);
	validate('phone', signUpRequestDto.phone);

	const response = await fetch("/api/admin/public/signUp/second", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify(signUpRequestDto)
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
 * 관리자 회원 탈퇴
 */
export async function withdrawal(password) {
	validate('password', password);

	const response = await fetch("/api/admin/private/withdrawal", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ password })
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

/**
 * 이메일 찾기 (휴대폰 인증 토큰 통해 이메일을 찾는다.)
 */
export async function findAdminEmail(phone) {

	const response = await fetch("/api/admin/public/find/email", {
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

/**
 * 비밀번호 찾기 1단계 이메일 검사 
 */
export async function isEmailExistsForFindPassword(email) {

	validate('email', email);

	const response = await fetch("/api/admin/public/find/pw", {
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

/**
 * 비밀번호 찾기
 */
export async function getAccessToUpdatePasswordByPhone(email, phone) {
	validate("email", email);
	validate("phone", phone);

	const response = await fetch("/api/admin/public/find/pw/by/phone", {
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

/**
 * 비밀번호 찾기 2단계 
 */
export async function getAccessToUpdatePasswordByEmail(email) {
	validate("email", email);

	const response = await fetch("/api/admin/public/find/pw/by/email", {
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

/**
 * 비밀번호 찾기 3단계 비밀번호 변경
 */
export async function updatePasswordForFindPassword(password, confirmPassword) {
	validate('password', password);
	validate('password', confirmPassword);

	if (password !== confirmPassword) {
		throw new ValidationError("두 비밀번호가 일치하지 않습니다.");
	}

	const response = await fetch("/api/admin/public/find/pw/update", {
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
