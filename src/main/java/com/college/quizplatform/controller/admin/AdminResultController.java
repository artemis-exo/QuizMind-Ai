package com.college.quizplatform.controller.admin;

import com.college.quizplatform.dto.admin.AdminResultResponse;
import com.college.quizplatform.model.Attempt;
import com.college.quizplatform.model.User;
import com.college.quizplatform.repository.UserRepository;
import com.college.quizplatform.service.AttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/results")
@RequiredArgsConstructor
public class AdminResultController {

    private final AttemptService attemptService;
    private final UserRepository userRepository;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    @GetMapping("/{sessionId}")
    public List<AdminResultResponse> getResults(@PathVariable String sessionId) {

        // Use the paginated method (0, 1000) which is authorized for Admins
        List<Attempt> attempts = attemptService.getLeaderboard(sessionId, 0, 1000).getContent();

        Set<String> studentIds = attempts.stream()
                .map(Attempt::getStudentId)
                .collect(Collectors.toSet());

        Map<String, User> userMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        AtomicInteger rankCounter = new AtomicInteger(1);

        return attempts.stream()
                .map(a -> {
                    User user = userMap.get(a.getStudentId());
                    String name = (user != null) ? user.getName() : "Unknown User";
                    String email = (user != null) ? user.getEmail() : "Unknown Email";

                    return AdminResultResponse.builder()
                            .rank(rankCounter.getAndIncrement())
                            .studentId(a.getStudentId())
                            .studentName(name)
                            .studentEmail(email)
                            .score(a.getScore())
                            .submitted(a.isSubmitted())
                            .build();
                })
                .toList();
    }
}