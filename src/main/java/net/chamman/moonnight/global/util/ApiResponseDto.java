package net.chamman.moonnight.global.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.chamman.moonnight.global.exception.HttpStatusCode;

@Schema(description = "기본 응답 포맷")
@Getter
@AllArgsConstructor
public class ApiResponseDto<T> {
	
	@Schema(description = "HTTP 상태 코드", example = "200")
    private final int status;

    @Schema(description = "커스텀 응답 코드", example = "2000")
    private final String code;

    @Schema(description = "응답 메시지", example = "요청 성공.")
    private final String message;

    @Schema(description = "응답 데이터", nullable = true)
    private final T data;

    private ApiResponseDto(HttpStatusCode httpStatusCode, String message, T data) {
        this.status = httpStatusCode.getStatus();
        this.code = httpStatusCode.getCode();
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponseDto<T> of(HttpStatusCode httpStatusCode, String message, T data) {
        return new ApiResponseDto<>(httpStatusCode, message, data);
    }
    
//    public static <T> ApiResponseDto<T> of(HttpStatusCode httpStatusCode, T data) {
//        return new ApiResponseDto<>(httpStatusCode, null, data);
//    }
	
}
