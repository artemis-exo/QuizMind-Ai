package com.college.quizplatform.service.ai;

public class AiPromptBuilder {

    public static String buildQuizPrompt(String topic, int count) {

        return """
            Generate %d multiple choice questions about "%s".

            STRICT RULES:
            - Return ONLY a valid JSON array.
            - No markdown. No explanation. No conversational text.
            - DO NOT generate "What is the output of this code?" questions. 
            - DO NOT include large code snippets. Focus entirely on conceptual, theoretical, and architectural knowledge.
            - Each object MUST contain exactly these keys:
                "question": (string) The text of the question
                "options": (array of 4 strings) The multiple choice options
                "correctOption": (integer) The 0-based index of the correct option (0, 1, 2, or 3)
                "marks": (integer) The marks for this question (default to 4)

            Example:
            [
              {
                "question": "Which of the following is not a Java feature?",
                "options": ["Dynamic", "Architecture Neutral", "Use of pointers", "Object-oriented"],
                "correctOption": 2,
                "marks": 4
              }
            ]

            If format cannot be followed return [].
            """.formatted(count, topic);
    }
}