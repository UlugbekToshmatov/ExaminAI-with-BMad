package com.examinai.review;

import java.util.Objects;

/**
 * Option for mentor queue filter {@code <select>}: id + display label.
 */
public final class MentorQueueLabelValue {

    private final long id;
    private final String label;

    public MentorQueueLabelValue(long id, String label) {
        this.id = id;
        this.label = label;
    }

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MentorQueueLabelValue that = (MentorQueueLabelValue) o;
        return id == that.id && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }
}
