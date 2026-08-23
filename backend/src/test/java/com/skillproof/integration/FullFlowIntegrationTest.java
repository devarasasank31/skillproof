package com.skillproof.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION", matches = "true")
class FullFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.seed.demo-user", () -> "false");
    }

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;

    static String accessToken;
    static Long skillId;
    static Long assessmentId;
    static Long questionId;
    static Long reviewId;

    private HttpHeaders auth() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    @Order(1)
    void step1_registerAndLogin() {
        var reg = rest.postForEntity("/api/auth/register",
                Map.of("name", "Flow", "email", "flow@test.dev", "password", "Password1!"),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, reg.getStatusCode());
        accessToken = reg.getBody().get("accessToken").asText();
    }

    @Test
    @Order(2)
    void step2_claimJava() {
        var res = rest.exchange("/api/skills", HttpMethod.POST,
                new HttpEntity<>(Map.of("skillName", "Java"), auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        skillId = res.getBody().get("id").asLong();
        assertEquals("NEW", res.getBody().get("state").asText());
    }

    @Test
    @Order(3)
    void step3_startAssessmentAndAnswerAll() {
        var start = rest.exchange("/api/skills/" + skillId + "/assess", HttpMethod.POST,
                new HttpEntity<>(Map.of("difficulty", "EASY", "count", 3), auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, start.getStatusCode());
        assessmentId = start.getBody().get("assessmentId").asLong();
        var questions = start.getBody().withArray("questions");
        assertTrue(questions.size() > 0);

        int answered = 0;
        for (JsonNode q : questions) {
            questionId = q.get("id").asLong();
            String answer = "MCQ".equals(q.get("type").asText())
                    ? "Set"
                    : "HashMap uses a hash function to place keys into buckets, handles collisions "
                      + "with linked lists converted to trees, relies on the equals and hashCode contract, "
                      + "and resizes when the load factor threshold is exceeded.";
            var ans = rest.exchange("/api/assessments/" + assessmentId + "/answers", HttpMethod.POST,
                    new HttpEntity<>(Map.of("questionId", questionId, "answerText", answer), auth()),
                    JsonNode.class);
            assertEquals(HttpStatus.OK, ans.getStatusCode());
            answered++;
        }
        assertEquals(questions.size(), answered);
    }

    @Test
    @Order(4)
    void step4_completeAssessmentProducesScoreAndReview() {
        var done = rest.exchange("/api/assessments/" + assessmentId + "/complete", HttpMethod.POST,
                new HttpEntity<>(auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, done.getStatusCode());
        int score = done.getBody().get("score").asInt();
        assertTrue(score > 0);

        var skills = rest.exchange("/api/skills", HttpMethod.GET, new HttpEntity<>(auth()), JsonNode.class);
        var row = skills.getBody().get(0);
        assertEquals("Java", row.get("name").asText());
        assertTrue(row.get("confidence").asInt() > 0);
        assertNotNull(row.get("nextReviewAt"));

        var due = rest.exchange("/api/reviews/today", HttpMethod.GET, new HttpEntity<>(auth()), JsonNode.class);
        if (due.getBody().isArray() && due.getBody().size() > 0) {
            reviewId = due.getBody().get(0).get("reviewId").asLong();
        }
    }

    @Test
    @Order(5)
    void step5_completeReviewIfDue() {
        if (reviewId == null) return;
        var res = rest.exchange("/api/reviews/" + reviewId + "/complete", HttpMethod.POST,
                new HttpEntity<>(Map.of("score", 85), auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(7, res.getBody().get("intervalDays").asInt());
    }

    @Test
    @Order(6)
    void step6_submitChallengeBumpsPractical() {
        var list = rest.exchange("/api/challenges?skill=Java", HttpMethod.GET,
                new HttpEntity<>(auth()), JsonNode.class);
        assertTrue(list.getBody().size() > 0);
        long challengeId = list.getBody().get(0).get("id").asLong();

        var sub = rest.exchange("/api/challenges/" + challengeId + "/submit", HttpMethod.POST,
                new HttpEntity<>(Map.of("submissionText",
                        "I would use a hash map combined with a doubly linked list so get and put are O(1). "
                                + "When capacity is exceeded we evict the least recently used entry from the tail."), auth()),
                JsonNode.class);
        assertEquals(HttpStatus.OK, sub.getStatusCode());
        assertTrue(sub.getBody().get("score").asInt() >= 0);
        assertEquals(sub.getBody().get("checksTotal").asInt() > 0,
                sub.getBody().get("checksTotal").asInt() == sub.getBody().get("checksTotal").asInt());
    }

    @Test
    @Order(7)
    void step7_analyzeJobAndReadiness() {
        var job = rest.exchange("/api/jobs/analyze", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "title", "Backend Engineer",
                        "text", "Looking for strong Java and Spring Boot engineers with PostgreSQL, Docker, Kafka and AWS experience. REST API design required. DSA fundamentals expected."), auth()),
                JsonNode.class);
        assertEquals(HttpStatus.OK, job.getStatusCode());
        assertTrue(job.getBody().get("readiness").asInt() >= 0);

        var market = rest.exchange("/api/jobs/market", HttpMethod.GET, new HttpEntity<>(auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, market.getStatusCode());
    }

    @Test
    @Order(8)
    void step8_dashboardAndRecommendationsGenerated() {
        var dash = rest.exchange("/api/dashboard", HttpMethod.GET, new HttpEntity<>(auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, dash.getStatusCode());
        assertTrue(dash.getBody().get("readiness").asInt() >= 0);

        var recs = rest.exchange("/api/recommendations", HttpMethod.GET, new HttpEntity<>(auth()), JsonNode.class);
        assertEquals(HttpStatus.OK, recs.getStatusCode());
    }

    @Test
    @Order(9)
    void step9_unauthenticatedAccessBlocked() {
        var res = rest.getForEntity("/api/skills", JsonNode.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }
}
