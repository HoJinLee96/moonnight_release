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
import net.chamman.moonnight.domain.answer.admin.dto.AdminAnswerResponseDto;
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
	
	@Transactional
	public AdminAnswerResponseDto registerAnswer(int adminId, AdminAnswerCreateRequestDto dto, String clientIp) {
		Admin admin = adminService.getActiveAdminByAdminId(adminId);
        Question question = questionRepository.findById(dto.questionId())
                .orElseThrow(() -> new NoSuchDataException(QUESTION_NOT_FOUND));
        
		Answer answer = Answer.builder()
				.question(question)
				.admin(admin)
	            .content(dto.content())
	            .clientIp(clientIp)
	            .build();
		answerRepository.save(answer);
		return convertToDto(answer, adminId);
	}
	
	@ActiveAdminOnly
	@Transactional
	public AdminAnswerResponseDto modifyAnswer(int adminId, int answerId, AdminAnswerModifyRequestDto dto) {
		Answer answer = findAnswerWithAuthById(adminId, answerId, dto.version());
		answer.modify(dto.content());
		
		return convertToDto(answer, adminId);
	}
	
	@ActiveAdminOnly
	@Transactional
	public void deleteAnswer(int adminId, int answerId, AdminAnswerDeleteRequestDto dto) {
		Answer answer = findAnswerWithAuthById(adminId, answerId, dto.version());
		answerRepository.delete(answer);
	}
	
    private Answer findAnswerWithAuthById(int adminId, int answerId, int version) {
    	Answer answer = answerRepository.findByIdWithAdmin(answerId)
                .orElseThrow(() -> new NoSuchDataException(ANSWER_NOT_FOUND));
        if (!answer.verifyAdmin(adminId)) {
            throw new ForbiddenException(AUTHORIZATION_FAILED, "답변 작성자가 일치하지 않음.");
        }
        answer.verifyVersion(version);
        return answer;
    }

    private AdminAnswerResponseDto convertToDto(Answer answer, int adminId) {
        int encodedId = obfuscator.encode(answer.getAnswerId());
        return AdminAnswerResponseDto.from(answer, encodedId, adminId);
    }
    
}
