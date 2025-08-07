
export async function signInAuthPhone() {
	const response = await fetch("/api/sign/public/in/auth/sms", {
		method: "POST"
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

export async function signInAuthEmail() {
	const response = await fetch("/api/sign/public/in/auth/email", {
		method: "POST"
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


export async function signOutAuth() {
	const response = await fetch("/api/sign/private/out/auth", {
		method: "POST"
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