package com.skillproof.assessment;

import com.skillproof.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping("/skills/{skillId}/assess")
    public AssessmentService.StartedAssessment start(@CurrentUserId Long userId,
        @PathVariable Long skillId,
        @Valid @RequestBody(required = false) AssessmentService.StartRequest req) {
        return service.start(userId, skillId, req == null ? new AssessmentService.StartRequest(null, null) : req);
    }

    @PostMapping("/assessments/{id}/answers")
    public AssessmentService.AnswerResult answer(@CurrentUserId Long userId,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody AssessmentService.AnswerRequest req) {
        return service.answer(userId, id, req);
    }

    @PostMapping("/assessments/{id}/complete")
    public AssessmentService.CompletedResult complete(@CurrentUserId Long userId, @PathVariable Long id) {
        return service.complete(userId, id);
    }
}


