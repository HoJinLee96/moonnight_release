package net.chamman.moonnight.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/question")
public class QuestionViewController {

    @GetMapping
    public String showQuestionBoard() {
        return "question/questionBoard";
    }
    
    @GetMapping("/verification")
    public String showQuestionVerification() {
        return "question/questionVerification";
    }
    
    @GetMapping("/{questionId}")
    public String showQuestionView(@PathVariable int questionId, Model model) {
        model.addAttribute("questionId", questionId);
        return "question/questionView";
    }
    
    @GetMapping("/register")
    public String showRegisterForm() {
        return "question/questionRegister";
    }

}
