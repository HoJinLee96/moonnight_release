function validateComment(comment) {
	if (comment.commentText == "") {
		const error = new Error("댓글을 입력해 주세요.");
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	} else if (comment.estimateId == "" ) {
		const error = new Error("올바른 요청이 아닙니다.");
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
}

export async function registerComment(commentDto) {

	validateComment(commentDto);

	const response = await fetch("/api/comment/private/register", {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(commentDto)
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

export async function updateAddress(commentId, commentDto) {

	validateComment(commentDto);

	const response = await fetch("/api/comment/private/" + commentId, {
		method: "PATCH",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(commentDto)
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

export async function deleteAddress(commentId) {
	const response = await fetch("/api/comment/private/" + commentId, {
		method: "DELETE",
	});

	if (response.ok) {
		successHandler();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}