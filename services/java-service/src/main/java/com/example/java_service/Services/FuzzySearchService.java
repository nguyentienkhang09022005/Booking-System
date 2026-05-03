package com.example.java_service.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FuzzySearchService {

    private static final double EXACT_MATCH_SCORE = 100.0;
    private static final double UNACCENT_MATCH_SCORE = 70.0;
    private static final double SIMILARITY_THRESHOLD = 0.5;
    private static final double SIMILARITY_MATCH_SCORE = 50.0;

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public String removeAccents(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String removed = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return removed.toLowerCase().trim();
    }

    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.isBlank() || s2.isBlank()) {
            return 0.0;
        }
        
        String str1 = removeAccents(s1).toLowerCase();
        String str2 = removeAccents(s2).toLowerCase();
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        double jaroWinkler = jaroWinklerSimilarity(str1, str2);
        double levenshtein = 1.0 - (double) levenshteinDistance(str1, str2) / Math.max(str1.length(), str2.length());
        
        return Math.max(jaroWinkler, levenshtein);
    }

    private double jaroWinklerSimilarity(String s1, String s2) {
        double jaro = jaroSimilarity(s1, s2);
        int prefixLen = 0;
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
        
        for (int i = 0; i < maxPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLen++;
            } else {
                break;
            }
        }
        
        return jaro + (prefixLen * 0.1 * (1 - jaro));
    }

    private double jaroSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }
        
        int matchDistance = Math.max(len1, len2) / 2 - 1;
        if (matchDistance < 0) {
            matchDistance = 0;
        }
        
        boolean[] matched1 = new boolean[len1];
        boolean[] matched2 = new boolean[len2];
        
        int matches = 0;
        int transpositions = 0;
        
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            
            for (int j = start; j < end; j++) {
                if (matched2[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                matched1[i] = true;
                matched2[j] = true;
                matches++;
                break;
            }
        }
        
        if (matches == 0) {
            return 0.0;
        }
        
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!matched1[i]) {
                continue;
            }
            while (!matched2[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }
        
        return (double) (matches + matches) / (len1 + len2) + 
               (double) (matches - transpositions / 2) / matches;
    }

    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }

    public <T> List<T> filterAndReRankByFuzzyScore(
            List<T> results,
            String keyword,
            Function<T, String>... textExtractors) {
        
        if (results == null || results.isEmpty() || keyword == null || keyword.isBlank()) {
            return results;
        }

        if (textExtractors == null || textExtractors.length == 0) {
            return results;
        }

        String normalizedKeyword = removeAccents(keyword);
        String upperKeyword = keyword.toUpperCase();

        Map<T, Double> scoreMap = new HashMap<>();

        for (T item : results) {
            double maxScore = 0.0;
            boolean hasMatch = false;

            for (Function<T, String> extractor : textExtractors) {
                String text = extractor.apply(item);
                if (text == null || text.isBlank()) {
                    continue;
                }

                double fieldScore = calculateFieldScore(text, keyword, normalizedKeyword, upperKeyword);
                if (fieldScore > 0) {
                    hasMatch = true;
                }
                maxScore = Math.max(maxScore, fieldScore);
            }

            if (hasMatch) {
                scoreMap.put(item, maxScore);
            }
        }

        return results.stream()
                .filter(item -> scoreMap.containsKey(item))
                .sorted((a, b) -> {
                    double scoreA = scoreMap.getOrDefault(a, 0.0);
                    double scoreB = scoreMap.getOrDefault(b, 0.0);
                    if (Double.compare(scoreB, scoreA) != 0) {
                        return Double.compare(scoreB, scoreA);
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    public <T> List<T> reRankByFuzzyScore(
            List<T> results,
            String keyword,
            Function<T, String>... textExtractors) {
        
        if (results == null || results.isEmpty() || keyword == null || keyword.isBlank()) {
            return results;
        }

        if (textExtractors == null || textExtractors.length == 0) {
            return results;
        }

        String normalizedKeyword = removeAccents(keyword);
        String upperKeyword = keyword.toUpperCase();

        Map<T, Double> scoreMap = new HashMap<>();

        for (T item : results) {
            double maxScore = 0.0;

            for (Function<T, String> extractor : textExtractors) {
                String text = extractor.apply(item);
                if (text == null || text.isBlank()) {
                    continue;
                }

                double fieldScore = calculateFieldScore(text, keyword, normalizedKeyword, upperKeyword);
                maxScore = Math.max(maxScore, fieldScore);
            }

            scoreMap.put(item, maxScore);
        }

        return results.stream()
                .sorted((a, b) -> {
                    double scoreA = scoreMap.getOrDefault(a, 0.0);
                    double scoreB = scoreMap.getOrDefault(b, 0.0);
                    if (Double.compare(scoreB, scoreA) != 0) {
                        return Double.compare(scoreB, scoreA);
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    private double calculateFieldScore(String fieldValue, String keyword, String normalizedKeyword, String upperKeyword) {
        double score = 0.0;
        
        if (fieldValue.toUpperCase().contains(upperKeyword)) {
            score = Math.max(score, EXACT_MATCH_SCORE);
        }
        
        String normalizedFieldValue = removeAccents(fieldValue);
        if (normalizedFieldValue.contains(normalizedKeyword)) {
            score = Math.max(score, UNACCENT_MATCH_SCORE);
        }
        
        double similarity = calculateSimilarity(keyword, fieldValue);
        if (similarity >= SIMILARITY_THRESHOLD) {
            score = Math.max(score, SIMILARITY_MATCH_SCORE * similarity);
        }
        
        return score;
    }
}