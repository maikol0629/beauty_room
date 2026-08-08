package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.dto.review.ReviewRequestDto;
import com.mr.sb.beauty_room.dto.review.ReviewResponseDto;
import com.mr.sb.beauty_room.services.IReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final IReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReviewResponseDto> create(@Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.ok(reviewService.save(request));
    }

    @GetMapping("/stylist/{stylistId}")
    public ResponseEntity<?> getByStylist(@PathVariable Long stylistId) {
        List<ReviewResponseDto> reviews = reviewService.findByStylistId(stylistId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getByClient(@PathVariable Long clientId) {
        List<ReviewResponseDto> reviews = reviewService.findByClientId(clientId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/stylist/{stylistId}/average")
    public ResponseEntity<Double> getAverage(@PathVariable Long stylistId) {
        return ResponseEntity.ok(reviewService.averageRatingByStylistId(stylistId));
    }
}
