package com.resumematcher.service;

import com.resumematcher.entity.RoleProfile;
import com.resumematcher.repository.RoleProfileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches a resume against all predefined RoleProfiles when
 * no job description is provided.
 *
 * Reuses KeywordMatchService so the matching logic is in one place.
 * This service only adds the "loop over profiles + sort" layer.
 */
@Service
public class ResumeOnlyAnalysisService {

    private final RoleProfileRepository roleProfileRepository;
    private final KeywordMatchService keywordMatchService;

    public ResumeOnlyAnalysisService(RoleProfileRepository roleProfileRepository,
                                     KeywordMatchService keywordMatchService) {
        this.roleProfileRepository = roleProfileRepository;
        this.keywordMatchService = keywordMatchService;
    }

    /**
     * Compares the resume text against every RoleProfile and returns
     * results sorted by match percentage (highest first).
     *
     * Each result is a Map with: roleName, description, matchPercentage,
     * matchedKeywords, missingKeywords.
     *
     * Algorithm:
     *   1. Load all RoleProfiles from the database.
     *   2. For each profile, call KeywordMatchService.compare() using the
     *      profile's keyword string as the "JD text".
     *   3. Collect every result into a list.
     *   4. Sort descending by matchPercentage so the best fit is first.
     */
    public List<Map<String, Object>> analyzeAgainstAllRoles(String resumeText) {
        List<RoleProfile> profiles = roleProfileRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (RoleProfile profile : profiles) {
            // profile.getKeywords() is "java, spring, hibernate, ..."
            // KeywordMatchService.extractKeywords() splits on non-alphanumeric,
            // so commas act as separators — it works naturally.
            Map<String, Object> matchResult = keywordMatchService.compare(
                    resumeText, profile.getKeywords());

            // Wrap the match data with the role's name and description
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("roleName", profile.getRoleName());
            entry.put("description", profile.getDescription());
            entry.put("matchPercentage", matchResult.get("matchPercentage"));
            entry.put("matchedKeywords", matchResult.get("matchedKeywords"));
            entry.put("missingKeywords", matchResult.get("missingKeywords"));

            results.add(entry);
        }

        // Sort descending: the role with the highest match comes first
        results.sort(Comparator.comparingDouble(
                (Map<String, Object> r) -> (Double) r.get("matchPercentage"))
                .reversed());

        return results;
    }
}
