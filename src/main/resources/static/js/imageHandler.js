const MAX_IMAGES = 10;
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

/**
 * 이미지 처리를 위한 핸들러 인스턴스를 생성합니다.
 * @param {object} config - 설정 객체
 * @param {string} config.previewContainerId - 미리보기 이미지가 표시될 컨테이너의 ID
 * @param {string} config.inputElementId - <input type="file"> 요소의 ID
 * @param {string[]} [config.initialImageUrls=[]] - (수정 시 사용) 기존 이미지 URL 배열
 * @returns {object} 이미지 핸들러 인스턴스
 */
export function createImageHandler(config) {
	// --- 1. 상태 관리 변수 ---
	const previewContainer = document.getElementById(config.previewContainerId);
	const inputElement = document.getElementById(config.inputElementId);

	let existingImageUrls = config.initialImageUrls ? [...config.initialImageUrls] : [];
	let newImageFiles = [];
	let deletedImageUrls = [];

	// --- 2. 내부 헬퍼 함수 ---

	// [핵심] 미리보기 영역을 모두 다시 그리는 함수
	function _redrawPreviews() {
		previewContainer.innerHTML = ''; // 초기화

		// 기존 이미지(URL) 그리기
		existingImageUrls.forEach(url => {
			const wrapper = _createPreviewElement(url, 'url', url);
			previewContainer.appendChild(wrapper);
		});

		// 새로 추가된 파일 그리기
		newImageFiles.forEach((file, index) => {
			const localUrl = URL.createObjectURL(file);
			const wrapper = _createPreviewElement(localUrl, 'file', index);
			previewContainer.appendChild(wrapper);
		});
	}

	// 미리보기 DOM 요소를 생성하는 함수
	function _createPreviewElement(src, type, identifier) {
		const wrapper = document.createElement('div');
		wrapper.className = 'image-preview-item';
		wrapper.innerHTML = `
            <img src="${src}" alt="미리보기 이미지">
            <button type="button" class="delete-image-btn" data-type="${type}" data-identifier="${identifier}">X</button>
        `;
		return wrapper;
	}

	// 파일 선택 이벤트를 처리하는 함수
	async function _handleFileSelection(event) {
		const files = Array.from(event.target.files);
		if (files.length === 0) return;

		const totalCount = existingImageUrls.length + newImageFiles.length + files.length;
		if (totalCount > MAX_IMAGES) {
			alert(`이미지는 최대 ${MAX_IMAGES}장까지 업로드할 수 있습니다.`);
			// 입력값 초기화
			inputElement.value = "";
			return;
		}

		// 압축 로직 (기존 코드와 유사)
		const compressionPromises = files.map(file => {
			if (file.size > MAX_FILE_SIZE) {
				alert(`이미지 1장의 최대 용량은 10MB입니다. (${file.name} 제외)`);
				return Promise.resolve(null);
			}
			return new Promise((resolve, reject) => {
				new Compressor(file, {
					quality: 0.8, maxWidth: 1920, maxHeight: 1080,
					success: resolve,
					error: reject,
				});
			});
		});

		try {
			const compressedFiles = await Promise.all(compressionPromises);
			newImageFiles.push(...compressedFiles.filter(f => f !== null));
		} catch (error) {
			console.error('이미지 압축 실패:', error);
			alert('이미지 처리 중 오류가 발생했습니다.');
		} finally {
			_redrawPreviews();
			// 입력값 초기화
			inputElement.value = "";
		}
	}

	// 삭제 버튼 클릭을 처리하는 함수
	function _handleDeleteClick(event) {
		const button = event.target.closest('.delete-image-btn');
		if (!button) return;

		const type = button.dataset.type;
		const identifier = button.dataset.identifier;

		if (type === 'url') {
			existingImageUrls = existingImageUrls.filter(url => url !== identifier);
			deletedImageUrls.push(identifier);
		} else if (type === 'file') {
			newImageFiles.splice(parseInt(identifier, 10), 1);
		}

		_redrawPreviews();
	}

	// --- 3. 공개 API (핸들러 인스턴스가 노출할 기능) ---
	return {
		// 핸들러를 초기화하고 이벤트 리스너를 연결
		init() {
			if (!previewContainer || !inputElement) {
				console.error('ImageHandler 초기화 실패: 필수 DOM 요소가 없습니다.');
				return;
			}
			inputElement.addEventListener('change', _handleFileSelection);
			previewContainer.addEventListener('click', _handleDeleteClick);
			_redrawPreviews(); // 초기 상태 그리기
		},
		
		buildRegisterFormData(dto) {
			const formData = new FormData();

			// 1. DTO 객체를 JSON 문자열로 변환하여 추가
			const finalDto = { ...dto};
			formData.append('dto', new Blob([JSON.stringify(finalDto)], { type: 'application/json' }));

			// 2. 새로 추가된 파일들을 추가
			newImageFiles.forEach(file => {
				formData.append('images', file);
			});

			return formData;
		},

		// 최종 제출을 위한 FormData 객체를 생성하여 반환
		buildFormData(dto) {
			const formData = new FormData();

			// 1. DTO 객체를 JSON 문자열로 변환하여 추가
			const finalDto = { ...dto, deletedImagesPath: deletedImageUrls };
			formData.append('dto', new Blob([JSON.stringify(finalDto)], { type: 'application/json' }));

			// 2. 새로 추가된 파일들을 추가
			newImageFiles.forEach(file => {
				formData.append('images', file);
			});

			return formData;
		}
	};
}