package com.resumematcher.controller;

import com.resumematcher.dto.MlPredictionResponse;
import com.resumematcher.entity.AnalysisResult;
import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.Resume;
import com.resumematcher.entity.User;
import com.resumematcher.repository.AnalysisResultRepository;
import com.resumematcher.repository.JobDescriptionRepository;
import com.resumematcher.repository.ResumeRepository;
import com.resumematcher.repository.UserRepository;
import com.resumematcher.service.KeywordMatchService;
import com.resumematcher.service.MlClientService;
import com.resumematcher.service.ResumeParserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles the full resume-vs-JD analysis flow:
 *
 *   GET  /analyze  → show the upload form
 *   POST /analyze  → parse resume, match keywords, call ML, save result, show result
 *
 * This controller orchestrates the services but contains no business logic itself.
 * All heavy lifting is delegated to:
 *   - ResumeParserService   (PDF/TXT → text)
 *   - KeywordMatchService   (keyword overlap %)
 *   - MlClientService       (Flask ML prediction)
 */
@Controller
public class AnalysisController {

    private final ResumeParserService resumeParserService;
    private final KeywordMatchService keywordMatchService;
    private final MlClientService mlClientService;
    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserRepository userRepository;

    // Constructor injection — Spring resolves all 7 dependencies automatically
    public AnalysisController(ResumeParserService resumeParserService,
                              KeywordMatchService keywordMatchService,
                              MlClientService mlClientService,
                              ResumeRepository resumeRepository,
                              JobDescriptionRepository jobDescriptionRepository,
                              AnalysisResultRepository analysisResultRepository,
                              UserRepository userRepository) {
        this.resumeParserService = resumeParserService;
        this.keywordMatchService = keywordMatchService;
        this.mlClientService = mlClientService;
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.userRepository = userRepository;
    }

    // ── Show the analysis form ────────────────────────────────────

    @GetMapping("/analyze")
    public String showAnalyzeForm() {
        return "analyze"; // templates/analyze.html
    }

    // ── Process the analysis ──────────────────────────────────────

    /**
     * Full analysis flow triggered when the user submits the form.
     *
     * @param resumeFile       the uploaded PDF or TXT file
     * @param jobTitle         optional short title for the JD (defaults to "Untitled")
     * @param jobDescriptionText  the full job description text
     * @param principal        injected by Spring Security — holds the logged-in username
     * @param model            Thymeleaf model to pass data to the result template
     */
    @PostMapping("/analyze")
    public String analyzeResume(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam(value = "jobTitle", required = false) String jobTitle,
            @RequestParam("jobDescription") String jobDescriptionText,
            Principal principal,
            Model model) {

        // ── Validate job description ───────────────────────────────
        if (jobDescriptionText == null || jobDescriptionText.isBlank()) {
            model.addAttribute("error", "Job description cannot be empty.");
            return "analyze";
        }

        // ── Step 1: Extract text from the uploaded resume ──────────
        String resumeText;
        try {
            resumeText = resumeParserService.extractText(resumeFile);
        } catch (IllegalArgumentException e) {
            // User error: empty file, wrong type, too large, scanned PDF
            model.addAttribute("error", e.getMessage());
            return "analyze";
        } catch (RuntimeException e) {
            // System error: IO failure, corrupt file
            model.addAttribute("error", "Failed to read the file. Please try again.");
            return "analyze";
        }

        // ── Step 2: Get the logged-in user ─────────────────────────
        // principal.getName() returns the username from the security session.
        // We load the full User entity because Resume and JobDescription
        // have a @ManyToOne FK to User.
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ── Step 3: Save the Resume entity ─────────────────────────
        Resume resume = new Resume();
        resume.setFileName(resumeFile.getOriginalFilename());
        resume.setExtractedText(resumeText);
        resume.setUploadedAt(LocalDateTime.now());
        resume.setUser(user);
        resumeRepository.save(resume);

        // ── Step 4: Save the JobDescription entity ─────────────────
        JobDescription jd = new JobDescription();
        jd.setTitle(jobTitle != null && !jobTitle.isBlank() ? jobTitle : "Untitled");
        jd.setDescription(jobDescriptionText);
        jd.setUser(user);
        jobDescriptionRepository.save(jd);

        // ── Step 5: Keyword matching ───────────────────────────────
        // Reuses the existing KeywordMatchService — no duplication.
        Map<String, Object> matchResult = keywordMatchService.compare(
                resumeText, jobDescriptionText);

        // ── Step 6: ML prediction ──────────────────────────────────
        // If Flask is down, mlResult contains the fallback ("Unavailable", 0.0).
        MlPredictionResponse mlResult = mlClientService.predict(resumeText);

        // ── Step 7: Save AnalysisResult ────────────────────────────
        AnalysisResult result = new AnalysisResult();
        result.setResume(resume);
        result.setJobDescription(jd);
        result.setMatchPercentage((Double) matchResult.get("matchPercentage"));
        result.setMatchedKeywords((String) matchResult.get("matchedKeywords"));
        result.setMissingKeywords((String) matchResult.get("missingKeywords"));
        result.setPredictedCategory(mlResult.getCategory());
        result.setAnalyzedAt(LocalDateTime.now());
        analysisResultRepository.save(result);

        // ── Step 8: Pass data to the result template ───────────────
        model.addAttribute("result", result);
        model.addAttribute("resumeFileName", resumeFile.getOriginalFilename());

        // confidence and top3 are NOT stored in the database (see note below),
        // so we pass them directly to the template via the Model.
        model.addAttribute("confidence", mlResult.getConfidence());
        model.addAttribute("top3", mlResult.getTop3());

        return "result"; // templates/result.html
    }
}
