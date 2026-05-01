package com.college.quizplatform.controller;

import com.college.quizplatform.dto.ai.AiQuestionResponse;
import com.college.quizplatform.security.CustomUserDetails;
import com.college.quizplatform.service.QuizAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQuizController {

    private final QuizAiService quizAiService;

    // Legacy endpoint for previewing questions
    @GetMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'STUDENT')")
    public List<AiQuestionResponse> generateQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count) {

        return quizAiService.generateQuiz(topic, count);
    }

    // NEW: Generates the quiz, saves it to the DB, and returns the Session ID
    @PostMapping("/generate-and-start")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'STUDENT')")
    public Map<String, String> generateAndStartQuiz(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String studentId = userDetails.getUserId();

        String sessionId = quizAiService.generateAndCreateSession(topic, count, studentId);

        return Map.of("sessionId", sessionId);
    }
}