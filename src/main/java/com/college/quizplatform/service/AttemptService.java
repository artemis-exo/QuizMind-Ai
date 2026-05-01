package com.college.quizplatform.service;

import com.college.quizplatform.dto.attempt.StartAttemptResponse;
import com.college.quizplatform.dto.attempt.SubmitAnswerRequest;
import com.college.quizplatform.model.Attempt;
import com.college.quizplatform.model.Question;
import com.college.quizplatform.model.Quiz;
import com.college.quizplatform.model.QuizSession;
import com.college.quizplatform.repository.AttemptRepository;
import com.college.quizplatform.repository.QuestionRepository;
import com.college.quizplatform.repository.QuizRepository;
import com.college.quizplatform.repository.QuizSessionRepository;
import com.college.quizplatform.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final QuizSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    // ================= START ATTEMPT =================
    @Transactional
    public StartAttemptResponse startAttempt(String sessionId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("User not authenticated properly");
        }

        String studentId = userDetails.getUserId();

        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Instant now = Instant.now();

        if (now.isBefore(session.getScheduledStartTime())) throw new RuntimeException("Session has not started yet");
        if (now.isAfter(session.getScheduledEndTime()) || session.isEnded()) throw new RuntimeException("Session expired");

        attemptRepository.findBySessionIdAndStudentId(sessionId, studentId)
                .ifPresent(a -> { throw new RuntimeException("You have already started this quiz."); });

        Attempt attempt = Attempt.builder()
                .sessionId(sessionId)
                .quizId(session.getQuizId())
                .studentId(studentId)
                .answers(new HashMap<>())
                .questionStartTimes(new HashMap<>())
                .startedAt(now)
                .submitted(false)
                .score(0)
                .build();

        Attempt saved = attemptRepository.save(attempt);

        return StartAttemptResponse.builder()
                .attemptId(saved.getId())
                .sessionId(sessionId)
                .quizId(saved.getQuizId())
                .build();
    }

    // ================= SAVE ANSWER =================
    @Transactional
    public void submitAnswer(SubmitAnswerRequest request) {

        Attempt attempt = attemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (attempt.isSubmitted()) throw new RuntimeException("Attempt already submitted");

        QuizSession session = sessionRepository.findById(attempt.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Instant now = Instant.now();
        if (now.isAfter(session.getScheduledEndTime()) || session.isEnded()) {
            throw new RuntimeException("Session time expired");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!attempt.getQuestionStartTimes().containsKey(request.getQuestionId())) {
            attempt.getQuestionStartTimes().put(request.getQuestionId(), now);
        }

        Instant startTime = attempt.getQuestionStartTimes().get(request.getQuestionId());

        if (startTime != null && question.getTimeLimitSeconds() > 0) {
            long secondsElapsed = now.getEpochSecond() - startTime.getEpochSecond();
            if (secondsElapsed > question.getTimeLimitSeconds()) {
                throw new RuntimeException("Time limit for this question has expired.");
            }
        }

        attempt.getAnswers().put(request.getQuestionId(), request.getSelectedOptionIndex());
        attemptRepository.save(attempt);
    }

    // ================= SUBMIT ATTEMPT =================
    @Transactional
    public int submitAttempt(String attemptId) {

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (attempt.isSubmitted()) throw new RuntimeException("Already submitted");

        Quiz quiz = quizRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // N+1 Optimization: Fetch all questions at once and store in memory
        List<Question> questions = questionRepository.findByQuizId(attempt.getQuizId());
        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int score = 0;

        for (var entry : attempt.getAnswers().entrySet()) {
            Question question = questionMap.get(entry.getKey());
            if (question == null) continue;

            if (question.getCorrectAnswerIndex() == entry.getValue()) {
                score += quiz.getMarksPerQuestion();
            } else {
                score -= quiz.getNegativeMarks();
            }
        }

        if (score < 0) score = 0;

        attempt.setScore(score);
        attempt.setSubmitted(true);
        attempt.setSubmittedAt(Instant.now());

        attemptRepository.save(attempt);
        return score;
    }

    // ================= LEADERBOARD =================
    public List<Attempt> getLeaderboard(String sessionId) {
        return attemptRepository.findBySessionIdAndSubmittedTrueOrderByScoreDescSubmittedAtAsc(sessionId);
    }

    public Page<Attempt> getLeaderboard(String sessionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("score"), Sort.Order.asc("submittedAt")));
        return attemptRepository.findBySessionIdAndSubmittedTrue(sessionId, pageable);
    }
}