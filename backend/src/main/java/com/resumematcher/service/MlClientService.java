package com.resumematcher.service;

import com.resumematcher.dto.MlPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Calls the Flask ML service to predict a job category from resume text.
 *
 * Uses Spring's RestTemplate — a synchronous HTTP client that sends a
 * request and blocks until the response arrives.  Simple and sufficient
 * for our use case (one call per analysis, sub-second latency).
 *
 * If Flask is down or returns an error, we return a safe fallback
 * instead of crashing the entire Spring Boot application.
 */
@Service
public class MlClientService {

    private static final Logger logger = LoggerFactory.getLogger(MlClientService.class);

    // Flask runs on this URL by default (set in ml-service/app.py)
    private static final String ML_SERVICE_URL = "http://127.0.0.1:5000/predict";

    // RestTemplate is thread-safe: one instance can be shared across requests
    private final RestTemplate restTemplate;

    public MlClientService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends resume text to Flask and returns the prediction.
     *
     * Flow:
     *   1. Build a JSON body: {"text": "resume text here..."}
     *   2. POST it to Flask's /predict endpoint.
     *   3. Deserialize the JSON response into MlPredictionResponse.
     *   4. If anything fails → return a fallback with "Unavailable".
     *
     * @param resumeText the cleaned text extracted from the uploaded resume
     * @return prediction result (never null — fallback is returned on error)
     */
    public MlPredictionResponse predict(String resumeText) {
        try {
            // Build the request body as a Map; Jackson serializes it to JSON
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", resumeText);

            // postForEntity sends POST, deserializes the response body
            // into MlPredictionResponse using Jackson
            ResponseEntity<MlPredictionResponse> response =
                    restTemplate.postForEntity(
                            ML_SERVICE_URL,
                            requestBody,
                            MlPredictionResponse.class);

            MlPredictionResponse body = response.getBody();
            if (body != null) {
                logger.info("ML prediction: {} (confidence: {})",
                        body.getCategory(), body.getConfidence());
                return body;
            }

        } catch (Exception e) {
            // Catches: ConnectException (Flask down), HttpClientErrorException
            // (4xx), HttpServerErrorException (5xx), timeout, JSON parse error.
            // We log the error but do NOT re-throw — the analysis can still
            // return keyword-match results even without the ML prediction.
            logger.warn("ML service unavailable: {}", e.getMessage());
        }

        // ── Fallback: Flask is down or returned an invalid response ──
        return createFallback();
    }

    /**
     * Returns a safe default when the ML service cannot be reached.
     * The controller can check for "Unavailable" to show a message.
     */
    private MlPredictionResponse createFallback() {
        MlPredictionResponse fallback = new MlPredictionResponse();
        fallback.setCategory("Unavailable");
        fallback.setConfidence(0.0);
        fallback.setTop3(new ArrayList<>());
        return fallback;
    }
}
