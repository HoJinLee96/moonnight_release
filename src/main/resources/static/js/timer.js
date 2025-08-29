// /js/timer.js

// 타이머의 상태를 관리하는 중앙 객체
const authTimerManager = {
    intervalId: null,      // setInterval을 저장할 변수
    endTime: null,         // 타이머가 끝나는 정확한 시간
    displayElement: null,  // 시간을 표시할 HTML 요소
    onTimeoutCallback: null, // 시간 만료 시 실행할 함수

    /**
     * 타이머를 시작하거나 재개합니다.
     * @param {number} durationInSeconds - 타이머 지속 시간(초)
     * @param {function} onTimeout - 타임아웃 시 실행할 콜백 함수
     */
    start(durationInSeconds, onTimeout) {
        // 이미 실행 중인 타이머가 있다면 건드리지 않음
        if (this.intervalId) return;

        console.log(`타이머를 ${durationInSeconds}초로 시작합니다.`);
        this.endTime = Date.now() + durationInSeconds * 1000;
        this.onTimeoutCallback = onTimeout;

        // 1초마다 update 함수를 실행
        this.intervalId = setInterval(this.update.bind(this), 1000);
        this.update(); // 즉시 한 번 실행해서 바로 시간이 표시되게 함
    },

    /**
     * 남은 시간을 계산하고 UI를 업데이트합니다.
     */
    update() {
        if (!this.endTime) return;

        const remainingMs = this.endTime - Date.now();

        if (remainingMs <= 0) {
            // 시간이 다 됐을 경우
            if (this.displayElement) {
                this.displayElement.textContent = "시간만료";
            }
            if (this.onTimeoutCallback) {
                this.onTimeoutCallback(); // 저장해둔 콜백 함수 실행
            }
            this.stop(); // 타이머 완전 정지
        } else {
            // 아직 시간이 남았을 경우
            if (this.displayElement) {
                const totalSeconds = Math.round(remainingMs / 1000);
                const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
                const seconds = String(totalSeconds % 60).padStart(2, '0');
                this.displayElement.textContent = `${minutes}:${seconds}`;
            }
        }
    },

    /**
     * 시간을 표시할 DOM 요소를 지정하거나 변경합니다.
     * @param {HTMLElement} element - 시간을 표시할 요소
     */
    setDisplay(element) {
        this.displayElement = element;
        this.update(); // 새로운 요소를 즉시 업데이트
    },

    /**
     * 타이머를 완전히 정지하고 모든 상태를 초기화합니다.
     */
    stop() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }
        // 모든 상태 초기화
        this.intervalId = null;
        this.endTime = null;
        this.displayElement = null;
        this.onTimeoutCallback = null;
        console.log("타이머가 중지되었습니다.");
    }
};

// 다른 파일에서 사용할 수 있도록 export
export { authTimerManager };

// --------------------------------------

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