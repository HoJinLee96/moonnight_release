package net.chamman.moonnight.domain.question;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.answer.dto.AnswerResponseDto;
import net.chamman.moonnight.domain.question.dto.QuestionCreateRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionDeleteRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionModifyRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionPasswordRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionResponseDto;
import net.chamman.moonnight.domain.question.dto.QuestionSimpleResponseDto;
import net.chamman.moonnight.global.context.CustomRequestContextHolder;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

	private final QuestionService questionService;
	private final ApiResponseFactory apiResponseFactory;
	private final Obfuscator obfuscator;

	@Operation(summary = "질문 등록", description = "질문 등록")
	@PostMapping("/register")
	public ResponseEntity<ApiResponseDto<QuestionResponseDto>> registerQuestion(
			@Valid @RequestBody QuestionCreateRequestDto dto) {

		String clientIp = CustomRequestContextHolder.getClientIp();

		Question question = questionService.registerQuestion(dto, clientIp);

		return ResponseEntity.ok(apiResponseFactory.success(CREATE_SUCCESS, convertToDto(question)));
	}

	@Operation(summary = "질문 리스트 조회", description = "질문 리스트 조회")
	@GetMapping
	public ResponseEntity<ApiResponseDto<List<QuestionSimpleResponseDto>>> getQuestionsByPage(Pageable pageable) {

		List<Question> list = questionService.getQuestionsByPage(pageable);

		List<QuestionSimpleResponseDto> body = list.stream().map(this::convertToSimpleDto).toList();

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(READ_SUCCESS, body));
	}

	@Operation(summary = "질문 검색 리스트 조회", description = "질문 검색 리스트 조회")
	@GetMapping("/search")
	public ResponseEntity<ApiResponseDto<List<QuestionSimpleResponseDto>>> getQuestionsByPage(
			@RequestParam String title, Pageable pageable) {

		List<Question> list = questionService.getQuestionsByTitle(title, pageable);

		List<QuestionSimpleResponseDto> body = list.stream().map(this::convertToSimpleDto).toList();

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(READ_SUCCESS, body));
	}

	@Operation(summary = "질문 비밀번호 입력 조회", description = "질문 비밀번호 입력 조회")
	@PostMapping("/{questionId}/verification")
	public ResponseEntity<ApiResponseDto<QuestionResponseDto>> verifyPasswordForView(@PathVariable int questionId,
			@Valid @RequestBody QuestionPasswordRequestDto dto) {

		Question question = questionService.verifyPasswordForModification(questionId, dto);

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(SUCCESS, convertToDto(question)));
	}

	@Operation(summary = "질문 수정", description = "질문 수정")
	@PatchMapping("/{questionId}")
	public ResponseEntity<ApiResponseDto<QuestionResponseDto>> modifyQuestion(@PathVariable int questionId,
			@Valid @RequestBody QuestionModifyRequestDto dto) {

		Question question = questionService.modifyQuestion(questionId, dto);

		return ResponseEntity.status(HttpStatus.OK)
				.body(apiResponseFactory.success(UPDATE_SUCCESS, convertToDto(question)));
	}

	@Operation(summary = "질문 삭제", description = "질문 삭제")
	@DeleteMapping("/{questionId}")
	public ResponseEntity<ApiResponseDto<Void>> deleteQuestion(@PathVariable int questionId,
			@Valid @RequestBody QuestionDeleteRequestDto dto) {

		questionService.deleteQuestion(questionId, dto);

		return ResponseEntity.status(HttpStatus.OK).body(apiResponseFactory.success(DELETE_SUCCESS));
	}

	private QuestionResponseDto convertToDto(Question question) {
		int encodedId = obfuscator.encode(question.getQuestionId());
		List<AnswerResponseDto> answerDtos = question.getAnswers().stream()
				.map(answer -> new AnswerResponseDto(obfuscator.encode(answer.getAnswerId()), answer.getContent(),
						answer.getCreatedAt()))
				.toList();
		return QuestionResponseDto.from(question, encodedId, answerDtos);
	}

	private QuestionSimpleResponseDto convertToSimpleDto(Question question) {
		int encodedId = obfuscator.encode(question.getQuestionId());
		return QuestionSimpleResponseDto.from(question, encodedId);
	}

}
