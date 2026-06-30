package com.example.evaluation.controller;

import com.example.evaluation.common.Result;
import com.example.evaluation.service.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/check/{submissionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> check(@PathVariable Long submissionId) {
        log.info("触发智能核查: submissionId={}", submissionId);
        return Result.success("核查完成", verificationService.verify(submissionId));
    }

    @GetMapping("/result/{submissionId}")
    public Result<?> getResult(@PathVariable Long submissionId) {
        return Result.success(verificationService.getResults(submissionId));
    }
}
