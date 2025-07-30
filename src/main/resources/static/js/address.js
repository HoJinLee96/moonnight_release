function validateAddress(address) {
	if (address.postcode == "" || address.mainAddress == "") {
		const error = new Error("주소를 입력해 주세요.");
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	} else if (address.detailAddress == "") {
		const error = new Error("상세 주소를 입력해 주세요.");
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
}

export async function getMyAddress(addressId) {
	const response = await fetch("/api/address/private/" + addressId, {
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

export async function getMyAddressList() {
	const response = await fetch("/api/address/private/getList", {
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
export async function registerAddress(address) {

	validateAddress(address);

	const response = await fetch("/api/address/private/register", {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(address)
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

export async function updateAddress(addressId, address) {

	validateAddress(address);

	const response = await fetch("/api/address/private/" + addressId, {
		method: "PATCH",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(address)
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

export async function updatePrimary(addressId) {
	const response = await fetch("/api/address/private/primary/" + addressId, {
		method: "PATCH",
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

export async function deleteAddress(addressId) {
	const response = await fetch("/api/address/private/" + addressId, {
		method: "DELETE",
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