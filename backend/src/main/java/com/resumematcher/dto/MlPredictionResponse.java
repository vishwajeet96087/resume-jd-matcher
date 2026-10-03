package com.resumematcher.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the JSON response from Flask's POST /predict endpoint.
 *
 * Flask returns:
 * {
 *   "category": "INFORMATION-TECHNOLOGY",
 *   "confidence": 0.87,
 *   "top3": [
 *     {"category": "INFORMATION-TECHNOLOGY", "probability": 0.87},
 *     {"category": "ENGINEERING",            "probability": 0.06},
 *     {"category": "BUSINESS-DEVELOPMENT",   "probability": 0.03}
 *   ]
 * }
 *
 * Jackson (included with spring-boot-starter-web) automatically deserializes
 * the JSON into this class because the field names match the JSON keys.
 */
public class MlPredictionResponse {

    private String category;
    private double confidence;
    private List<Top3Entry> top3 = new ArrayList<>();

    /**
     * Represents one entry in the top-3 list.
     * Static inner class keeps related code together without a separate file.
     */
    public static class Top3Entry {
        private String category;
        private double probability;

        public Top3Entry() {
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public double getProbability() {
            return probability;
        }

        public void setProbability(double probability) {
            this.probability = probability;
        }
    }

    public MlPredictionResponse() {
    }

    // ── Getters and Setters ───────────────────────────────────────

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<Top3Entry> getTop3() {
        return top3;
    }

    public void setTop3(List<Top3Entry> top3) {
        this.top3 = top3;
    }
}
