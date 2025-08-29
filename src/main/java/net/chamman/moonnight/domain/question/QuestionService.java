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
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.question.Question.QuestionStatus;
import net.chamman.moonnight.domain.question.dto.QuestionCreateRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionDeleteRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionModifyRequestDto;
import net.chamman.moonnight.domain.question.dto.QuestionPasswordRequestDto;
import net.chamman.moonnight.global.exception.MismatchPasswordException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.status.StatusDeleteException;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

	private final QuestionRepository questionRepository;
	private final Obfuscator obfuscator;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public Question registerQuestion(QuestionCreateRequestDto dto, String clientIp) {
		Question question = Question.builder().password(passwordEncoder.encode(dto.password())).title(dto.title())
				.content(dto.content()).questionStatus(QuestionStatus.PENDING).clientIp(clientIp).build();
		questionRepository.save(question);
		return question;
	}

	@Transactional(readOnly = true)
	public List<Question> getQuestionsByPage(Pageable pageable) {
		Page<Question> questionPage = questionRepository.findAllByQuestionStatusNot(QuestionStatus.DELETE, pageable);

		return questionPage.getContent();
	}

	@Transactional(readOnly = true)
	public List<Question> getQuestionsByTitle(String title, Pageable pageable) {
		Page<Question> questionPage = questionRepository.findByTitleContainingAndQuestionStatusNot(title,
				QuestionStatus.DELETE, pageable);

		return questionPage.getContent();
	}

	@Transactional(readOnly = true)
	public Question verifyPasswordForModification(int questionId, QuestionPasswordRequestDto dto) {
		Question question = findQuestionById(questionId);

		boolean match = passwordEncoder.matches(dto.password(), question.getPassword());
		if (match) {
			return question;
		} else {
			throw new MismatchPasswordException(QUESTION_PASSWORD_MISMATCH);
		}
	}

	@Transactional
	public Question modifyQuestion(int questionId, QuestionModifyRequestDto dto) {

		Question question = findQuestionWithAuth(questionId, dto.password(), dto.version());

		question.modify(dto.title(), dto.content());

		return question;
	}

	@Transactional
	public void deleteQuestion(int questionId, QuestionDeleteRequestDto dto) {

		Question question = findQuestionWithAuth(questionId, dto.password(), dto.version());

		question.updateStatusDelete();

	}

	private Question findQuestionById(int questionId) {
		Question question = questionRepository.findById(obfuscator.decode(questionId))
				.orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));
		if (question.isDelete()) { // 엔티티에 isDeleted() 같은 편의 메서드 추가 추천
			throw new StatusDeleteException(QUESTION_STATUS_DELETE, "삭제된 질문.");
		}
		return question;
	}

	private Question findQuestionWithAnswersById(int questionId) {
		log.debug("* questionId: [{}]", obfuscator.decode(questionId));
		Question question = questionRepository.findByIdWithAnswers(obfuscator.decode(questionId))
				.orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));
		if (question.isDelete()) {
			throw new StatusDeleteException(QUESTION_STATUS_DELETE, "삭제된 질문.");
		}
		return question;
	}

	private Question findQuestionWithAuth(int questionId, String rawPassword, int version) {
		Question question = findQuestionWithAnswersById(questionId);
		if (!passwordEncoder.matches(rawPassword, question.getPassword())) {
			throw new MismatchPasswordException(QUESTION_PASSWORD_MISMATCH);
		}
		question.verifyVersion(version);
		return question;
	}

}
