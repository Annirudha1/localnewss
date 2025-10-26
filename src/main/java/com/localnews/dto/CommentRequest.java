package com.localnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {

    @NotBlank(message = "Comment text is required")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String commentText;

    // Constructors
    public CommentRequest() {}

    public CommentRequest(String commentText) {
        this.commentText = commentText;
    }

    // Getters and setters
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
}
