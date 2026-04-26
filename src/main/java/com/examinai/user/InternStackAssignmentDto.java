package com.examinai.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable selection of stacks (create-intern, edit-intern-stacks).
 */
public class InternStackAssignmentDto {

    private List<Long> stackIds = new ArrayList<>();

    public List<Long> getStackIds() { return stackIds; }
    public void setStackIds(List<Long> stackIds) { this.stackIds = stackIds != null ? stackIds : new ArrayList<>(); }
}
