package net.chamman.moonnight.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminQuestionViewController {

	@GetMapping("/admin/question")
	public String showQuestionBoard() {
		
		return "admin/question/adminQuestionBoard";
	}
	
    @GetMapping("/admin/question/{questionId}")
    public String showQuestionView(@PathVariable int questionId, Model model) {
        model.addAttribute("questionId", questionId);
        return "admin/question/adminQuestionView";
    }
	
}
