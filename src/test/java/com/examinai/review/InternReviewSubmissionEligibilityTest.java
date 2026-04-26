package com.examinai.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternReviewSubmissionEligibilityTest {

    @Mock TaskReviewRepository taskReviewRepository;
    @InjectMocks InternReviewSubmissionEligibility eligibility;

    @Test
    void requireCanStartSubmission_approved_throws() {
        when(taskReviewRepository.existsByTask_IdAndIntern_IdAndStatus(1L, 2L, ReviewStatus.APPROVED))
            .thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> eligibility.requireCanStartSubmission(1L, 2L))
            .isInstanceOf(ReviewSubmissionBlockedException.class)
            .satisfies(ex -> assertThat(((ReviewSubmissionBlockedException) ex).getTaskId()).isEqualTo(1L));
    }
}
