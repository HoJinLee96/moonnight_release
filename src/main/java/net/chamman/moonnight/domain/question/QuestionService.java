package net.chamman.moonnight.domain.question;

import static net.chamman.moonnight.global.exception.HttpStatusCode.QUESTION_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.QUESTION_PASSWORD_MISMATCH;
import static net.chamman.moonnight.global.exception.HttpStatusCode.QUESTION_STATUS_DELETE;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.answer.dto.AnswerResponseDto;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;
import net.chamman.moonnight.domain.question.dto.QuestionCreateRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionDeleteRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionModifyRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionPasswordRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionResponseDto;
import net.chamman.moonnight.domain.question.dto.QuestionSimpleResponseDto;
import net.chamman.moonnight.global.exception.MismatchPasswordException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;

@Service
@RequiredArgsConstructor
public class QuestionService {
	
	private final QuestionRepository questionRepository;
	private final Obfuscator obfuscator;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public QuestionResponseDto registerQuestion(QuestionCreateRequestDto dto, String clientIp) {
	    Question question = Question.builder()
	            .password(passwordEncoder.encode(dto.password()))
	            .title(dto.title())
	            .content(dto.content())
	            .questionStatus(QuestionStatus.PENDING)
	            .clientIp(clientIp)
	            .build();
	    questionRepository.save(question);
		return convertToDto(question);
	}
	
	@Transactional(readOnly = true)
	public List<QuestionSimpleResponseDto> getQuestionsByPage(Pageable pageable) {
	    Page<Question> questionPage = questionRepository.findAllByQuestionStatusNot(QuestionStatus.DELETE, pageable);

	    return questionPage.getContent().stream()
	            .map(this::convertToSimpleDto)
	            .toList();
	}
	
	@Transactional(readOnly = true)
	public List<QuestionSimpleResponseDto> getQuestionsByTitle(String title, Pageable pageable) {
		Page<Question> questionPage  = questionRepository.findByTitleContainingAndQuestionStatusNot(title, QuestionStatus.DELETE, pageable);

        return questionPage.getContent().stream()
                .map(this::convertToSimpleDto)
                .toList();
	}
	
	@Transactional(readOnly = true)
	public QuestionResponseDto verifyPasswordForModification(int questionId, QuestionPasswordRequestDto dto) {
		Question question = findQuestionById(questionId);

		boolean match = passwordEncoder.matches(dto.password(), question.getPassword());
		if(match) {
			return convertToDto(question);
		} else {
			throw new MismatchPasswordException(QUESTION_PASSWORD_MISMATCH);
		}
	}
	
	@Transactional
	public QuestionResponseDto modifyQuestion(int questionId, QuestionModifyRequestDto dto) {
		Question question = findQuestionWithAuth(questionId, dto.password(), dto.version());

		question.modify(dto.title(), dto.content());
		
		return convertToDto(question);
	}
	
	@Transactional
	public QuestionResponseDto deleteQuestion(int questionId, QuestionDeleteRequestDto dto) {
		Question question = findQuestionWithAuth(questionId, dto.password(), dto.version());
		
		question.updateStatus(QuestionStatus.DELETE);
		
		return convertToDto(question);
	}
	
    private Question findQuestionById(int questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));
        if (question.isDelete()) { // 엔티티에 isDeleted() 같은 편의 메서드 추가 추천
            throw new StatusDeleteException(QUESTION_STATUS_DELETE, "삭제된 질문.");
        }
        return question;
    }

    private Question findQuestionWithAnswersById(int questionId) {
        Question question = questionRepository.findByIdWithAnswers(questionId)
                .orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));
        if (question.isDelete()) {
            throw new StatusDeleteException(QUESTION_STATUS_DELETE, "삭제된 질문.");
        }
        return question;
    }
    
    private Question findQuestionWithAuth(int questionId, String rawPassword, int version) {
        Question question = findQuestionWithAnswersById(questionId);
        question.verifyPassword(passwordEncoder, rawPassword);
        question.verifyVersion(version);
        return question;
    }
	
    private QuestionResponseDto convertToDto(Question question) {
        int encodedId = obfuscator.encode(question.getQuestionId());
        List<AnswerResponseDto> answerDtos = question.getAnswers().stream()
                .map(answer -> new AnswerResponseDto(
                		obfuscator.encode(answer.getAnswerId()),
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
}
