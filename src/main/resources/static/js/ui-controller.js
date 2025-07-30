export function passwordViewToggle(input, button) {
	if (input.type === 'password') {
		input.type = 'text';
		button.style.backgroundImage = "url('https://chamman.s3.ap-northeast-2.amazonaws.com/static/img/eyeIcon.png')";
	} else {
		input.type = 'password';
		button.style.backgroundImage = "url('https://chamman.s3.ap-northeast-2.amazonaws.com/static/img/eyeHiddenIcon.png')";
	}
}
export function inputValueInit(input, button) {
	input.value = '';
	button.style.display = "none";
}

export function buttonDisplay(input, button) {
	if (input.value) {
		button.style.display = "inline";
	} else {
		button.style.display = "none";
	}
}
export function openblankWindow(url) {
	// 새 창의 크기 지정
	const width = 500;
	const height = 800;

	// 창의 중앙 위치 계산
	const left = window.screenX + (window.outerWidth / 2) - (width / 2);
	const top = window.screenY + (window.outerHeight / 2) - (height / 2);

	// 새로운 창 열기
	window.open(
		url,  // 열고자 하는 URL
		'_blank',  // 새 창의 이름 또는 _blank (새 탭)
		'width=' + width + ',height=' + height + ',top=' + top + ',left=' + left  // 창의 크기와 위치
	);
	return false; // 기본 링크 동작을 막기 위해 false를 반환
}