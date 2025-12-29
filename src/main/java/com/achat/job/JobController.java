package com.achat.job;

import com.achat.common.ApiResponse;
import com.achat.job.dto.JobResponse;
import com.achat.job.dto.SelectJobRequest;
import com.achat.job.dto.UserJobResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> getAllJobs() {
        List<JobResponse> jobs = jobService.getAllJobs();
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @GetMapping("/selectable")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getSelectableJobs() {
        List<JobResponse> jobs = jobService.getSelectableJobs();
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserJobResponse>> getMyJob(
            @AuthenticationPrincipal String userId) {
        UserJobResponse response = jobService.getMyJob(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/select")
    public ResponseEntity<ApiResponse<UserJobResponse>> selectJob(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SelectJobRequest request) {
        UserJobResponse response = jobService.selectJob(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/exp")
    public ResponseEntity<ApiResponse<UserJobResponse>> addExp(
            @AuthenticationPrincipal String userId,
            @RequestParam int amount) {
        UserJobResponse response = jobService.addExp(userId, amount);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
