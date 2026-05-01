package com.college.quizplatform.service;

import com.college.quizplatform.dto.ai.AiQuestionResponse;
import com.college.quizplatform.model.Question;
import com.college.quizplatform.model.Quiz;
import com.college.quizplatform.model.QuizSession;
import com.college.quizplatform.repository.QuestionRepository;
import com.college.quizplatform.repository.QuizRepository;
import com.college.quizplatform.repository.QuizSessionRepository;
import com.college.quizplatform.service.ai.AiPromptBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuizAiService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSessionRepository sessionRepository;

    public List<AiQuestionResponse> generateQuiz(String topic, int count) {
        validateInput(topic, count);

        ChatClient chatClient = chatClientBuilder.build();
        String prompt = AiPromptBuilder.buildQuizPrompt(topic, count);

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseResponse(aiResponse);
    }

    // ================= GENERATE AND SAVE =================
    @Transactional
    public String generateAndCreateSession(String topic, int count, String studentId) {

        List<AiQuestionResponse> aiQuestions = generateQuiz(topic, count);
        int durationMinutes = count * 2;

        Quiz quiz = Quiz.builder()
                .title("AI Practice: " + topic)
                .topic(topic)
                .description("Custom AI-generated practice quiz.")
                .duration(durationMinutes)
                .active(true)
                .marksPerQuestion(4) // Or pull from aiQ.getMarks() if you prefer!
                .negativeMarks(1)
                .totalMarks(count * 4)
                .questionIds(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        quiz = quizRepository.save(quiz);

        List<Question> questionsToSave = new ArrayList<>();
        for (AiQuestionResponse aiQ : aiQuestions) {
            Question q = Question.builder()
                    .quizId(quiz.getId())
                    // FIX: Using getQuestion() to match your AiQuestionResponse.java
                    .questionText(aiQ.getQuestion())
                    .options(aiQ.getOptions())
                    // FIX: Using getCorrectOption() to match your AiQuestionResponse.java
                    .correctAnswerIndex(aiQ.getCorrectOption())
                    .category("AI-Generated")
                    .timeLimitSeconds(60)
                    .createdAt(Instant.now())
                    .build();
            questionsToSave.add(q);
        }

        List<Question> savedQuestions = questionRepository.saveAll(questionsToSave);

        for (Question q : savedQuestions) {
            quiz.getQuestionIds().add(q.getId());
        }
        quizRepository.save(quiz);

        QuizSession session = QuizSession.builder()
                .quizId(quiz.getId())
                .sessionName("AI Session: " + topic)
                .scheduledStartTime(Instant.now())
                .scheduledEndTime(Instant.now().plus(durationMinutes, ChronoUnit.MINUTES))
                .started(true)
                .ended(false)
                .allowedStudentIds(Set.of(studentId))
                .createdAt(Instant.now())
                .build();

        session = sessionRepository.save(session);

        return session.getId();
    }

    // ---------------- VALIDATION ----------------
    private void validateInput(String topic, int count) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("Topic required");
        if (topic.length() > 50) throw new IllegalArgumentException("Topic too long");
        if (count < 1 || count > 10) throw new IllegalArgumentException("Count must be 1-10");
    }

    // ---------------- JSON PARSING ----------------
    private List<AiQuestionResponse> parseResponse(String response) {
        try {
            String cleaned = response.replace("```json", "").replace("```", "").trim();
            int start = cleaned.indexOf("[");
            int end = cleaned.lastIndexOf("]") + 1;

            if (start == -1 || end == -1) throw new RuntimeException("AI did not return JSON array");

            String jsonOnly = cleaned.substring(start, end);

            return objectMapper.readValue(
                    jsonOnly,
                    new TypeReference<List<AiQuestionResponse>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("AI returned invalid format: " + response, e);
        }
    }
}