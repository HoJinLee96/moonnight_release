import { validate, ValidationError } from '/js/validate.js';

/**
 * 로그인
 */
export async function signIn(signInDto, rememberEmail) {

	validate('email', signInDto.email);
	validate('password', signInDto.password);

	let apiUrl = "/api/admin/sign/public/in";

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
	const response = await fetch("/api/admin/sign/private/out", {
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
 * LOCAL 회원가입 1차
 */
export async function signUpStep1(email, password, confirmPassword) {

	validate('email', email);
	validate('password', password);
	if (password !== confirmPassword) {
		throw new ValidationError('두 비밀번호가 일치하지 않습니다.');
	}

	const response = await fetch("/api/admin/sign/public/up/first", {
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
 * LOCAL 회원가입 2차
 */
export async function signUpStep2(signUpRequestDto) {

	validate('name', signUpRequestDto.name);
	validate('phone', signUpRequestDto.phone);

	const response = await fetch("/api/admin/sign/public/up/second", {
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

export async function withdrawal(password) {
	validate('password', password);

	const response = await fetch("/api/sign/private/delete", {
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