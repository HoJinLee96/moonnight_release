package net.chamman.moonnight.domain.question.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.QUESTION_NOT_FOUND;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.answer.dto.AnswerResponseDto;
import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;
import net.chamman.moonnight.domain.question.QuestionRepository;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionDeleteRequestDto;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionModifyRequestDto;
import net.chamman.moonnight.domain.question.admin.dto.AdminQuestionStatusUpdateRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionResponseDto;
import net.chamman.moonnight.domain.question.dto.QuestionSimpleResponseDto;
import net.chamman.moonnight.global.annotation.ActiveAdminOnly;
import net.chamman.moonnight.global.exception.NoSuchDataException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminQuestionService {
	
	private final QuestionRepository questionRepository;
	private final Obfuscator obfuscator;
	
	@Transactional(readOnly = true)
	public List<QuestionSimpleResponseDto> getQuestionsByPage(Pageable pageable) {

	    Page<Question> questionPage = questionRepository.findAll(pageable);

	    return questionPage.getContent().stream()
	            .map(this::convertToSimpleDto)
	            .toList();
	}
	
	@Transactional(readOnly = true)
	public List<QuestionSimpleResponseDto> getQuestionsByTitle(String title, Pageable pageable) {

		Page<Question> questionPage  = questionRepository.findByTitleContaining(title, pageable);

        return questionPage.getContent().stream()
                .map(this::convertToSimpleDto)
                .toList();
	}
	
	@Transactional
	@ActiveAdminOnly
	public QuestionResponseDto modifyQuestion(int adminId, int questionId, AdminQuestionModifyRequestDto dto) {

        Question question = findQuestionWithVersion(questionId, dto.version());
        
		question.modify(dto.title(), dto.content());
		
		return convertToDto(question);
	}
	
	@Transactional
	@ActiveAdminOnly
	public QuestionResponseDto updateQuestionStatus(int adminId, int questionId, AdminQuestionStatusUpdateRequestDto dto) {

        Question question = findQuestionWithVersion(questionId, dto.version());
		
		question.updateStatus(dto.questionStatus());
		
		return convertToDto(question);
	}
	
	@Transactional
	@ActiveAdminOnly
	public void deleteQuestion(int adminId, int questionId, AdminQuestionDeleteRequestDto dto) {

        Question question = findQuestionWithVersion(questionId, dto.version());
		
		question.updateStatus(QuestionStatus.DELETE);
	}
	
    private QuestionResponseDto convertToDto(Question question) {
        int encodedId = obfuscator.encode(question.getQuestionId());
        List<AnswerResponseDto> answerDtos = question.getAnswers().stream()
                .map(answer -> new AnswerResponseDto(
                		obfuscator.encode(answer.getAnswerId()), // 서비스 계층에서 직접 인코딩!
                        answer.getContent(),
                        answer.getCreatedAt()
                ))
                .toList();
        return QuestionResponseDto.from(question, encodedId, answerDtos);
    }
    
    private QuestionSimpleResponseDto convertToSimpleDto(Question question) {
    	int encodedId = obfuscator.encode(question.getQuestionId());
    	return QuestionSimpleResponseDto.from(question, encodedId);
    }
    
    private Question findQuestionWithVersion(int questionId, int version) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));
        question.verifyVersion(version);
        return question;
    }

}
