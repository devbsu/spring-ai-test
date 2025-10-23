package com.example.demo.dto;

import lombok.Data;

@Data
public class ReviewClassification {
    public enum Sentiment {
        POSITIVE, NEUTRAL, NEGATIVE
    }

    private String review;
    private Sentiment classification;
}
