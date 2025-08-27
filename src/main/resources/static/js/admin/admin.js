import { validate, ValidationError } from '/js/validate.js';

export async function updatePasswordByEmail(newPassword, confirmNewPassword, version) {
	validate('password', newPassword);
	validate('password', confirmNewPassword);

	if (newPassword !== confirmNewPassword) {
		throw new ValidationError("두 비밀번호가 일치하지 않습니다.");
	}

	const adminPasswordUpdateRequestDto = {
		newPassword: newPassword,
		confirmNewPassword: confirmNewPassword,
		version: version
	}

	const response = await fetch("/api/admin/private/update/pw/by/email", {
		method: "PATCH",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(adminPasswordUpdateRequestDto)
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

export async function updatePasswordByPhone(newPassword, confirmNewPassword, version) {
	validate('password', newPassword);
	validate('password', confirmNewPassword);

	if (newPassword !== confirmNewPassword) {
		throw new ValidationError("두 비밀번호가 일치하지 않습니다.");
	}

	const adminPasswordUpdateRequestDto = {
		newPassword: newPassword,
		confirmNewPassword: confirmNewPassword,
		version: version
	}

	const response = await fetch("/api/admin/private/update/pw/by/phone", {
		method: "PATCH",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(adminPasswordUpdateRequestDto)
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

export async function updatePhone(phone, version) {
	validate('phone', phone);
	
	const adminPhoneRequestDto = {
		phone: phone,
		version: version
	}

	const response = await fetch("/api/admin/private/update/phone", {
		method: "PATCH",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(adminPhoneRequestDto)
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


export async function updateName(name, version) {
	validate('name', name);
	
	const adminNameRequestDto = {
		name: name,
		version: version
	}

	const response = await fetch("/api/admin/private/update/name", {
		method: "PATCH",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(adminNameRequestDto)
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

