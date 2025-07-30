let currentPostcodePopup = null;

/**
 * Daum 우편번호 검색 API를 초기화하고 이벤트를 연결하는 함수
 * @param {object} config - 설정 객체
 * @param {string} config.triggerBtnId - 우편번호 검색을 실행할 버튼의 ID
 * @param {string} config.postcodeId - 우편번호를 입력할 input의 ID
 * @param {string} config.mainAddressId - 도로명 주소를 입력할 input의 ID
 * @param {string} config.detailAddressId - 상세 주소를 입력할 input의 ID
 */
export function initAddressSearch(config) {
	// 필수 설정값이 없으면 에러를 출력하고 종료
	if (!config.triggerBtnId || !config.postcodeId || !config.mainAddressId || !config.detailAddressId) {
		console.error('주소 검색 API 초기화 실패: 필수 설정값이 누락되었습니다.');
		return;
	}

	const triggerBtn = document.getElementById(config.triggerBtnId);
	const postcodeInput = document.getElementById(config.postcodeId);
	const mainAddressInput = document.getElementById(config.mainAddressId);

	if (!triggerBtn) {
		console.error(`주소 검색 트리거 버튼 ID '${config.triggerBtnId}'를 찾을 수 없습니다.`);
		return;
	}


	triggerBtn.addEventListener('click', () => openduamPostcode(triggerBtn, config));
	postcodeInput.addEventListener('click', () => openduamPostcode(triggerBtn, config));
	mainAddressInput.addEventListener('click', () => openduamPostcode(triggerBtn, config));
}

function openduamPostcode(triggerBtn, config) {
	if (currentPostcodePopup) {
		showFeedbackTooltip(triggerBtn, '이미 주소 검색창이 열려있습니다.');
	}

	// ★★★ 3. 새 팝업 객체를 생성하고, 변수에 할당 ★★★
	currentPostcodePopup = new daum.Postcode({
		oncomplete: function(data) {
			// 검색 결과 처리 함수 호출
			fillAddressFields(data, config);
			// 주소 선택 완료 시 onclose가 호출되므로, 여기서는 특별히 할 일 없음
		},

		// ★★★ 4. onclose 이벤트 핸들러 추가 ★★★
		// 팝업이 닫혔을 때 (주소를 선택했든, X를 눌렀든) 무조건 실행됨
		onclose: function(state) {
			// 팝업이 닫혔으니, 상태 변수를 다시 null로 만들어줌
			// 이렇게 해야 다음에 다시 '주소 찾기' 버튼을 눌렀을 때 새 팝업을 열 수 있음
			currentPostcodePopup = null;
		}
	});

	// 생성된 팝업을 화면에 띄움
	currentPostcodePopup.open({ popupKey: 'popup1' });
}

/**
 * 검색된 주소 데이터를 HTML input 필드에 채워 넣는 함수
 * @param {object} data - Daum 우편번호 검색 결과 데이터
 * @param {object} config - 초기화 시 사용된 설정 객체
 */
function fillAddressFields(data, config) {
	// 도로명 주소와 지번 주소 조합
	let addr = '';
	let extraAddr = '';

	if (data.userSelectedType === 'R') { // 사용자가 도로명 주소를 선택했을 경우
		addr = data.roadAddress;
	} else { // 사용자가 지번 주소를 선택했을 경우(J)
		addr = data.jibunAddress;
	}

	// 사용자가 선택한 주소가 도로명 타입일때 참고항목을 조합한다.
	if (data.userSelectedType === 'R') {
		// 법정동명이 있을 경우 추가한다. (법정리는 제외)
		// 법정동의 경우 마지막 문자가 "동/로/가"로 끝난다.
		if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)) {
			extraAddr += data.bname;
		}
		// 건물명이 있고, 공동주택일 경우 추가한다.
		if (data.buildingName !== '' && data.apartment === 'Y') {
			extraAddr += (extraAddr !== '' ? ', '
				+ data.buildingName : data.buildingName);
		}
		// 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
		if (extraAddr !== '') {
			extraAddr = ' (' + extraAddr + ')';
		}
		// 조합된 참고항목을 지번 주소 뒤에 붙인다.
		addr = addr + extraAddr;
	}

	// 설정 객체에 따라 해당 ID의 input에 값 채우기
	document.getElementById(config.postcodeId).value = data.zonecode;
	document.getElementById(config.mainAddressId).value = addr;
	document.getElementById(config.detailAddressId).focus();
}

/**
 * 사용자에게 피드백 메시지를 담은 말풍선을 보여주는 함수
 * @param {HTMLElement} targetElement - 말풍선이 나타날 기준 요소
 * @param {string} message - 보여줄 메시지 내용
 */
function showFeedbackTooltip(targetElement, message) {
	// 1. 말풍선(div) 요소를 동적으로 생성
	const tooltip = document.createElement('div');
	tooltip.className = 'feedback-tooltip';
	tooltip.textContent = message;

	// 2. body에 추가해서 화면에 보이게 준비
	document.body.appendChild(tooltip);

	// 3. 위치 계산
	const targetRect = targetElement.getBoundingClientRect();
	const tooltipRect = tooltip.getBoundingClientRect();

	// 버튼 바로 위에 중앙 정렬되도록 위치 설정
	tooltip.style.left = `${targetRect.left + (targetRect.width / 2) - (tooltipRect.width / 2)}px`;
	tooltip.style.top = `${targetRect.top + window.scrollY - tooltipRect.height - 10}px`; // 버튼 위 10px 여백
	// 4. 'show' 클래스를 추가해서 나타나는 애니메이션 실행
	//   - setTimeout을 살짝 주는 이유는, DOM에 추가된 후 애니메이션이 적용되도록 하기 위함
	setTimeout(() => {
		tooltip.classList.add('show');
	}, 10);

	// 5. 2.5초 후에 스르륵 사라지게 하고, DOM에서 완전히 제거
	setTimeout(() => {
		tooltip.classList.remove('show');
		// 애니메이션이 끝난 후 DOM에서 제거
		setTimeout(() => {
			if (tooltip.parentNode) {
				tooltip.parentNode.removeChild(tooltip);
			}
		}, 300); // transition 시간과 동일하게
	}, 2500);
}