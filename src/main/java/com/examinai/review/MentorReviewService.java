package com.examinai.review;

import com.examinai.user.UserAccount;
import com.examinai.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read-only queue and detail access for mentor/admin review pages.
 */
@Service
public class MentorReviewService {

    /** Use with queue queries to include all mentors (admin). */
    public static final long MENTOR_QUEUE_ALL_MENTORS = -1L;
    /** Optional filter: no intern or task filter. */
    public static final long FILTER_ID_ANY = 0L;

    private final TaskReviewRepository taskReviewRepository;
    private final UserAccountRepository userAccountRepository;

    public MentorReviewService(
        TaskReviewRepository taskReviewRepository,
        UserAccountRepository userAccountRepository) {
        this.taskReviewRepository = taskReviewRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public MentorReviewQueueView loadQueue(Authentication auth, String statusParam, String internIdParam, String taskIdParam) {
        ReviewStatus status = parseRequiredStatus(statusParam);
        long internFilter = parseRequiredOptionalLong("internId", internIdParam);
        long taskFilter = parseRequiredOptionalLong("taskId", taskIdParam);
        long mentorFilter = resolveMentorFilter(auth);
        List<TaskReview> reviews = taskReviewRepository.findMentorQueue(mentorFilter, status, internFilter, taskFilter);
        List<MentorQueueLabelValue> internOptions =
            taskReviewRepository.findMentorQueueInternOptions(mentorFilter, status, taskFilter);
        List<MentorQueueLabelValue> taskOptions =
            taskReviewRepository.findMentorQueueTaskOptions(mentorFilter, status, internFilter);
        return new MentorReviewQueueView(reviews, status, internFilter, taskFilter, internOptions, taskOptions);
    }

    /**
     * Loads a review for the placeholder detail page; enforces mentor scope (admin sees all).
     */
    @Transactional(readOnly = true)
    public TaskReview getReviewForDetailOrThrow(long reviewId, Authentication auth) {
        TaskReview tr = taskReviewRepository.findByIdWithTaskAndTaskMentor(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (isAdmin(auth)) {
            return tr;
        }
        UserAccount mentor = tr.getTask().getMentor();
        UserAccount current = userAccountRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (mentor.getId() == null || !mentor.getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return tr;
    }

    public boolean isAdmin(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private long resolveMentorFilter(Authentication auth) {
        if (isAdmin(auth)) {
            return MENTOR_QUEUE_ALL_MENTORS;
        }
        return userAccountRepository.findByUsername(auth.getName())
            .map(UserAccount::getId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }

    private ReviewStatus parseRequiredStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return ReviewStatus.LLM_EVALUATED;
        }
        try {
            return ReviewStatus.valueOf(statusParam.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter");
        }
    }

    private long parseRequiredOptionalLong(String paramName, String value) {
        if (value == null || value.isBlank()) {
            return FILTER_ID_ANY;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + paramName);
        }
    }
}
