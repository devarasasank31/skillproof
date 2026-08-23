package com.skillproof.assessment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuestionBank {

    public record BankQuestion(String skillName, String type, String difficulty, String prompt,
                               List<String> options, String answerKey, List<String> keywords, String explanation) {}

    private static final String RESOURCE = "questionbank.json";
    private final Map<String, List<BankQuestion>> bySkill;

    public QuestionBank(ObjectMapper mapper) {
        try {
            List<BankQuestion> all = mapper.readValue(
                    new ClassPathResource(RESOURCE).getInputStream(),
                    new TypeReference<List<BankQuestion>>() {});
            this.bySkill = all.stream().collect(Collectors.groupingBy(BankQuestion::skillName));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load question bank", e);
        }
    }

    public boolean hasSkill(String skillName) {
        return bySkill.containsKey(normalize(skillName));
    }

    public List<BankQuestion> forSkill(String skillName) {
        return bySkill.getOrDefault(normalize(skillName), List.of());
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
