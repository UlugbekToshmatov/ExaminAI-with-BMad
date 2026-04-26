package com.examinai.stack;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StackFormDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
