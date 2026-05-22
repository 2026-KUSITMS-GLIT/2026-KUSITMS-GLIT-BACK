package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.record.application.port.out.AiTaggingClient;
import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.StarRecord;

@ExtendWith(MockitoExtension.class)
class AiTaggingAsyncExecutorTest {

    private static final long JOB_ID = 1L;

    @Mock private AiTaggingClient aiTaggingClient;
    @Mock private AiTaggingJobPort aiTaggingJobPort;

    @InjectMocks private AiTaggingAsyncExecutor aiTaggingAsyncExecutor;

    @Test
    @DisplayName("성공 — 잡 조회 후 requestTagging 호출됨")
    void callsRequestTagging_whenJobFound() {
        StarRecord starRecord = new StarRecord();
        ReflectionTestUtils.setField(starRecord, "id", 10L);
        AiTaggingJob job = new AiTaggingJob();
        ReflectionTestUtils.setField(job, "id", JOB_ID);
        ReflectionTestUtils.setField(job, "starRecord", starRecord);
        given(aiTaggingJobPort.findById(JOB_ID)).willReturn(Optional.of(job));

        aiTaggingAsyncExecutor.execute(JOB_ID);

        verify(aiTaggingClient).requestTagging(job);
    }

    @Test
    @DisplayName("실패 — 잡 없으면 IllegalStateException")
    void throwsIllegalState_whenJobNotFound() {
        given(aiTaggingJobPort.findById(JOB_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiTaggingAsyncExecutor.execute(JOB_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jobId=" + JOB_ID);
    }
}
