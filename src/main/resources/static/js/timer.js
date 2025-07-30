// 실행 중인 타이머들을 관리하기 위한 객체
const activeTimers = {};

/**
 * 특정 DOM 요소에 카운트다운 타이머를 시작합니다.
 * @param {number} durationInSeconds - 타이머 지속 시간(초)
 * @param {HTMLElement} displayElement - 남은 시간을 표시할 DOM 요소
 * @param {function} [onTimeout] - 타이머가 종료됐을 때 실행할 콜백 함수 (선택 사항)
 */
export function startCountdown(durationInSeconds, displayElement, onTimeout) {
    // 만약 해당 요소에 이미 타이머가 돌고 있다면, 일단 정지시킨다.
    if (activeTimers[displayElement.id]) {
        clearInterval(activeTimers[displayElement.id]);
    }

    const endTime = Date.now() + durationInSeconds * 1000;

    const updateDisplay = () => {
        const remainingMs = endTime - Date.now();

        if (remainingMs <= 0) {
            stopCountdown(displayElement);
            displayElement.textContent = "시간만료";
            if (onTimeout) {
                onTimeout(); // 타임아웃 콜백 실행
            }
        } else {
            const totalSeconds = Math.round(remainingMs / 1000);
            const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
            const seconds = String(totalSeconds % 60).padStart(2, '0');
            displayElement.textContent = `${minutes}:${seconds}`;
        }
    };

    // 인터벌 ID를 객체에 저장
    activeTimers[displayElement.id] = setInterval(updateDisplay, 1000);
    updateDisplay(); // 즉시 한번 실행해서 03:00으로 보이게 함
}

/**
 * 특정 DOM 요소의 카운트다운 타이머를 정지시킵니다.
 * @param {HTMLElement} displayElement - 타이머가 연결된 DOM 요소
 */
export function stopCountdown(displayElement) {
    if (activeTimers[displayElement.id]) {
        clearInterval(activeTimers[displayElement.id]);
        delete activeTimers[displayElement.id]; // 관리 목록에서 제거
    }
}