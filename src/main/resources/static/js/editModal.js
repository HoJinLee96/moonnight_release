// --- 1. 모듈이 사용하는 모든 외부 함수들을 import ---
import { initAddressSearch } from '/js/address-api.js';
import { createImageHandler } from '/js/imageHandler.js';
import { sendEmail, verifyMailCode } from '/js/emailVerification.js';
import { sendSms, verifySmsCode } from '/js/smsVerification.js';
import { startCountdown, stopCountdown } from '/js/timer.js';
import { initPhoneFormatting, initVerificationCodeFormatting } from '/js/format.js';
import { clearEstimatePhone, updateEstimatePhoneByVerification, clearEstimateEmail, updateEstimateEmailByVerification } from '/js/estimate.js';

/**
 * 범용 견적 수정 모달을 생성하고 엽니다.
 * @param {object} estimateData - 수정할 견적 데이터 원본
 * @param {object} config - 페이지별 설정을 담은 객체
 * @param {function} config.updateApiFunction - '저장' 시 호출할 API 함수
 * @param {function} config.onSaveSuccess - 저장 성공 후 실행할 콜백 함수
 * @param {function} config.modalOverlayParents
 */
export function openEditModal(estimateData, config) {
	// --- 2. 상태 관리 변수 (원본 데이터를 복사해서 사용) ---
	const editState = JSON.parse(JSON.stringify(estimateData));
	let isPhoneVerifiedInModal = editState.phone !== "" && editState.phone !== null;
	let isEmailVerifiedInModal = editState.email !== "" && editState.email !== null;
	// --- 3. 모달 HTML 동적 생성 ---
	const modalOverlay = document.createElement('div');
	modalOverlay.className = 'edit-modal-overlay';
	// 모달 컨텐츠 HTML 구성
	modalOverlay.innerHTML = `
			        <div class="edit-modal-content">
			            <h3>견적 수정 (#${editState.estimateId})</h3>
			            <form id="edit-form">
							<div class="form-group-parents">
				                <div class="form-group form-child">
				                    <label class="form-label">이름</label>
				                    <input type="text" class="form-input" id="edit-name" value="${editState.name}">
				                </div>
				                <div class="form-group form-child">
				                    <label class="form-label">청소 서비스</label>
				                    <select class="form-select" id="edit-cleaningService"></select>
				                </div>
			                </div>
							<div class="form-group-parents">
				                <div class="form-group form-child">
				                    <label class="form-label">휴대폰</label>
				                    <div class="edit-auth-section" id="edit-phone-section"></div>
				                </div>
	
								<div class="form-group form-child">
				                    <label class="form-label">이메일</label>
				                    <div class="edit-auth-section" id="edit-email-section"></div>
				                </div>
							</div>
			                <div class="form-group">
			                    <label class="form-label">주소</label>
								<div class="edit-address-first-line">
								<input type="text" id="edit-postcode" name="postcode" class="form-input" placeholder="우편번호" readonly style="width: 100px;">
								<button type="button" class="btn btn--primary" id="address-search-btn" style="font-size: 14px;">주소 찾기</button>
								</div>
								<input type="text" id="edit-mainAddress" name="mainAddress" class="form-input" placeholder="주소"
									readonly style="margin-bottom: 5px;">
								<input type="text" id="edit-detailAddress" name="detailAddress" class="form-input"
									placeholder="상세주소 입력" >
							</div>
							
			                <div class="form-group">
			                    <label class="form-label">상세 내용</label>
			                    <textarea class="form-textarea" id="edit-content" rows="4">${editState.content || ''}</textarea>
			                </div>
			                <div class="form-group">
								<div class="edit-image">
				                    <label class="form-label">이미지 첨부 (최대 10장)</label>
				                    <input type="file" id="edit-image-input" multiple accept="image/*" style="display:none;">
				                    <label for="edit-image-input" class="btn btn-primary-mini">이미지 추가</label>
								</div>
			                    <div id="edit-image-preview"></div>
			                </div>

			                <div class="modal-actions">
			                    <button type="button" class="btn btn-secondary" id="cancel-edit">취소</button>
			                    <button type="submit" class="btn btn-primary-mini" id="save-edit">저장</button>
			                </div>
			            </form>
			        </div>
			    `;
	config.modalOverlayParents.appendChild(modalOverlay);

	// --- 4. 모달 내부 로직  ---

	// 서비스 select 채우기
	const serviceSelect = modalOverlay.querySelector('#edit-cleaningService');
	cleaningServices.forEach(service => {
		const option = document.createElement('option');
		option.value = service.name; // NEW_BUILDING 같은 enum name
		option.textContent = service.label; // 신축 청소 등 한글 레이블
		if (service.label === editState.cleaningService) {
			option.selected = true;
		}
		serviceSelect.appendChild(option);
	});

	// 주소 채우기 
	const editPostcode = modalOverlay.querySelector('#edit-postcode');
	const editMainAddress = modalOverlay.querySelector('#edit-mainAddress');
	const editDetailAddress = modalOverlay.querySelector('#edit-detailAddress')
	editPostcode.value = editState.postcode;
	editMainAddress.value = editState.mainAddress;
	editDetailAddress.value = editState.detailAddress;
	// 주소 검색 api 이벤트 설정
	const addressConfig = {
		triggerBtnId: 'address-search-btn', // '우편번호 찾기' 버튼 ID
		postcodeId: 'edit-postcode',             // 우편번호 input ID
		mainAddressId: 'edit-mainAddress',       // 도로명주소 input ID
		detailAddressId: 'edit-detailAddress',   // 상세주소 input ID
	};
	initAddressSearch(addressConfig);

	// 인증 섹션 그리는 함수 호출
	renderAuthSection('phone');
	renderAuthSection('email');
	// 인증 섹션 UI를 동적으로 그리는 함수
	function renderAuthSection(type) {
		const section = modalOverlay.querySelector(`#edit-${type}-section`);
		const isVerified = type === 'phone' ? isPhoneVerifiedInModal : isEmailVerifiedInModal;

		// 1. 인증된 상태의 UI 그리기
		if (isVerified) {
			section.innerHTML = `
			            <div class="verified-state input-with-button">
			                <input type="text" class="form-input" id="edit-${type}" value="${type === 'phone' ? editState.phone : editState.email}" readonly>
			                <button type="button" class="btn btn-primary-mini btn-sm" data-type="${type}" id="unverify-${type}-btn">인증 해제</button>
			            </div>
			        `;
			// '인증 해제' 버튼에 이벤트 리스너 추가
			section.querySelector(`#unverify-${type}-btn`).addEventListener('click', async () => {
				try {
					if (type === 'phone') {
						const json = await clearEstimatePhone(editState.estimateId, editState.version);
						isPhoneVerifiedInModal = false;
						editState.phone = ''; // 데이터도 비워주면 좋음
						config.onSaveSuccess(json.data);
						editState.version = json.data.version;
					} else {
						const json = await clearEstimateEmail(editState.estimateId, editState.version);
						isEmailVerifiedInModal = false;
						editState.email = '';
						config.onSaveSuccess(json.data);
						editState.version = json.data.version;
					}
					renderAuthSection(type); // UI 다시 그리기
				} catch (error) {
					alert(error.message);
				}
			});
			return;
		}

		// 2. 미인증 상태의 UI 그리기
		section.innerHTML = `
			        <div class="unverified-state input-with-button" id="send-section-${type}">
			            <input type="text" class="form-input" id="edit-${type}" placeholder="${type === 'phone' ? '휴대폰 번호' : '이메일 주소'}">
			            <button type="button" class="btn btn-primary-mini btn-sm" id="send-code-${type}-btn">인증번호 발송</button>
			        </div>
			        <div class="unverified-state input-with-button" id="verify-section-${type}" style="display: none;">
			            <input type="text" class="form-input" id="verify-code-${type}" placeholder="인증번호 6자리">
			            <span class="timer" id="timer-${type}">03:00</span>
			            <button type="button" class="btn btn-primary-mini btn-sm" id="verify-code-${type}-btn">확인</button>
			        </div>
					<div class="agreement-group">
						<input type="checkbox" id="${type}-agreement" name="agreement">
						<label for="${type}-agreement">개인정보 수집 및 이용 동의</label>
						<a href="javascript:;"
							onclick="javascript:footerlayerLoad('/static/infoAgreement.html'); return false;">[보기]</a>
					</div>
			    `;
		// 3. '인증번호 발송' 버튼에 이벤트 리스너 추가
		const sendBtn = section.querySelector(`#send-code-${type}-btn`);
		const valueInput = section.querySelector(`#edit-${type}`);
		const agreement = section.querySelector(`#${type}-agreement`);
		if (type === 'phone') {
			initPhoneFormatting(valueInput);
		}
		sendBtn.addEventListener('click', async () => {
			if (!agreement.checked) {
				alert("개인정보 수집 및 이용 동의를 체크해 주세요.");
				return;
			}
			const value = valueInput.value;
			sendBtn.disabled = true;
			sendBtn.textContent = '전송 중...';

			try {
				if (type === 'phone') {
					await sendSms(value);
				} else {
					await sendEmail(value);
				}
				alert('인증번호가 발송되었습니다.');

				// UI 변경: 인증번호 입력 칸 보여주기
				section.querySelector(`#send-section-${type}`).style.display = 'none';
				section.querySelector(`#verify-section-${type}`).style.display = 'flex';
				section.querySelector(`.agreement-group`).style.display = 'none';
				const timerEl = section.querySelector(`#timer-${type}`);
				startCountdown(180, timerEl, () => {
					section.querySelector(`#verify-code-${type}-btn`).disabled = true;
				});

			} catch (error) {
				alert(error.message);
				sendBtn.textContent = '인증번호 발송';
			} finally {
				sendBtn.disabled = false;
			}
		});

		// 4. '확인'(인증번호 검증) 버튼에 이벤트 리스너 추가
		const verifyBtn = section.querySelector(`#verify-code-${type}-btn`);
		const codeInput = section.querySelector(`#verify-code-${type}`);
		initVerificationCodeFormatting(codeInput);
		verifyBtn.addEventListener('click', async () => {
			const value = valueInput.value;
			const code = codeInput.value;
			verifyBtn.disabled = true;

			try {
				if (type === 'phone') {
					await verifySmsCode(value, code);
					const json = await updateEstimatePhoneByVerification(editState.estimateId, editState.version);
					config.onSaveSuccess(json.data);
					editState.version = json.data.version;
				} else {
					await verifyMailCode(value, code);
					const json = await updateEstimateEmailByVerification(editState.estimateId, editState.version);
					config.onSaveSuccess(json.data);
					editState.version = json.data.version;
				}
				alert('인증에 성공했습니다.');
				stopCountdown(section.querySelector(`#timer-${type}`));

				// ★★★ 핵심: 상태를 true로 바꾸고, UI를 다시 그린다! ★★★
				if (type === 'phone') {
					isPhoneVerifiedInModal = true;
					editState.phone = value; // 인증 성공한 값으로 데이터 업데이트
				} else {
					isEmailVerifiedInModal = true;
					editState.email = value;
				}
				renderAuthSection(type); // 성공했으니 인증된 상태의 UI로 새로고침

			} catch (error) {
				alert(error.message);
			} finally {
				verifyBtn.disabled = false;
			}
		});
	}

	// 이미지 핸들러 초기화
	const imageHandler = createImageHandler({
		previewContainerId: 'edit-image-preview',
		inputElementId: 'edit-image-input',
		initialImageUrls: editState.images
	});
	imageHandler.init();

	// 저장 처리 함수 (openEditModal 내부에 위치)
	async function handleSaveChanges(e) {
		e.preventDefault();

		const updatedDto = {
			name: modalOverlay.querySelector('#edit-name').value,
			postcode: editPostcode.value,
			mainAddress: editMainAddress.value,
			detailAddress: editDetailAddress.value,
			cleaningService: serviceSelect.value,
			content: modalOverlay.querySelector('#edit-content').value,
			version: editState.version
		};
		const formData = imageHandler.buildFormData(updatedDto);

		const saveBtn = modalOverlay.querySelector('#save-edit');
		saveBtn.disabled = true;
		saveBtn.textContent = '저장 중...';

		try {
			// 설정으로 넘겨받은 API 함수를 호출!
			const json = await config.updateApiFunction(editState.estimateId, formData);
			console.log(json);
			config.modalOverlayParents.removeChild(modalOverlay);

			// 설정으로 넘겨받은 콜백 함수를 실행!
			if (config.onSaveSuccess) {
				config.onSaveSuccess(json.data);
			}

		} catch (error) {
			alert(`수정 실패: ${error.message}`);
		} finally {
			saveBtn.disabled = false;
			saveBtn.textContent = '저장';
		}
	}


	// '취소' 버튼 이벤트 리스너
	modalOverlay.querySelector('#cancel-edit').addEventListener('click', () => {
		config.modalOverlayParents.removeChild(modalOverlay);
	});

	// '저장' 버튼 이벤트 리스너
	modalOverlay.querySelector('#edit-form').addEventListener('submit', handleSaveChanges);

}