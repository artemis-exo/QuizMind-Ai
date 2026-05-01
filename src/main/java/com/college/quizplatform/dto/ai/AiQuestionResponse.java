package com.college.quizplatform.dto.ai;

import java.util.List;

public class AiQuestionResponse {

    private String question;
    private List<String> options;
    private Integer correctOption;
    private Integer marks;

    // REQUIRED by Jackson
    public AiQuestionResponse() {
    }

    public AiQuestionResponse(String question,
                              List<String> options,
                              Integer correctOption,
                              Integer marks) {
        this.question = question;
        this.options = options;
        this.correctOption = correctOption;
        this.marks = marks;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Integer getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }
}