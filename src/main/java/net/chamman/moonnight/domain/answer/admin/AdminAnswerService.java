package net.chamman.moonnight.domain.answer.admin;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ANSWER_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.QUESTION_NOT_FOUND;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.admin.Admin;
import net.chamman.moonnight.domain.admin.AdminService;
import net.chamman.moonnight.domain.answer.Answer;
import net.chamman.moonnight.domain.answer.AnswerRepository;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerCreateRequestDto;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerDeleteRequestDto;
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerModifyRequestDto;
import net.chamman.moonnight.domain.question.Question;
import net.chamman.moonnight.domain.question.QuestionRepository;
import net.chamman.moonnight.global.annotation.ActiveAdminOnly;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;

@Service
@RequiredArgsConstructor
public class AdminAnswerService {

	private final AnswerRepository answerRepository;
	private final QuestionRepository questionRepository;
	private final AdminService adminService;
	private final Obfuscator obfuscator;

	@ActiveAdminOnly
	@Transactional
	public Answer registerAnswer(int adminId, AdminAnswerCreateRequestDto dto, String clientIp) {

		Admin admin = adminService.getActiveAdminById(adminId);

		Question question = questionRepository.findById(obfuscator.decode(dto.questionId()))
				.orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));

		Answer answer = Answer.builder().question(question).admin(admin).content(dto.content()).clientIp(clientIp)
				.build();
		answerRepository.save(answer);

		question.markAsAnswered();
		return answer;
	}

	@ActiveAdminOnly
	@Transactional
	public Answer modifyAnswer(int adminId, int answerId, AdminAnswerModifyRequestDto dto) {

		Answer answer = findAnswerWithAuthById(adminId, answerId, dto.version());

		answer.modify(dto.content());

		return answer;
	}

	@ActiveAdminOnly
	@Transactional
	public void deleteAnswer(int adminId, int answerId, AdminAnswerDeleteRequestDto dto) {

		Answer answer = findAnswerWithAuthById(adminId, answerId, dto.version());

		answerRepository.delete(answer);

		Question question = answer.getQuestion();
		if (answerRepository.countByQuestion_QuestionId(question.getQuestionId()) == 0) {
			question.revertToPending();
		}
	}

	private Answer findAnswerWithAuthById(int currentAdminId, int answerId, int version) {
		Answer answer = answerRepository.findByIdWithAdmin(obfuscator.decode(answerId))
				.orElseThrow(() -> new NoSuchDataException(ANSWER_NOT_FOUND));
		if (!answer.verifyAdmin(currentAdminId)) {
			throw new ForbiddenException(AUTHORIZATION_FAILED, "답변 작성자가 일치하지 않음.");
		}
		answer.verifyVersion(version);
		return answer;
	}

}
