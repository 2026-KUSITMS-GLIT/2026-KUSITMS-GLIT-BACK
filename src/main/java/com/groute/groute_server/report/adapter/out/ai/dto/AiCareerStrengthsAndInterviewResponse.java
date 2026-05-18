package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/career/strengths-and-interview 응답. */
public record AiCareerStrengthsAndInterviewResponse(
        List<StrengthItem> strengths,
        @JsonProperty("interview_questions") List<InterviewQuestion> interviewQuestions) {

    public record StrengthItem(
            String title,
            String description,
            List<EvidenceItem> evidences) {}

    public record InterviewQuestion(
            String question,
            List<EvidenceItem> evidences) {}

    public record EvidenceItem(
            int id,
            @JsonProperty("project_name") String projectName,
            @JsonProperty("scrum_title") String scrumTitle,
            @JsonProperty("created_at") String createdAt) {}
}
