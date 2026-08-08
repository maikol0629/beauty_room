package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.review.ReviewRequestDto;
import com.mr.sb.beauty_room.dto.review.ReviewResponseDto;

import java.util.List;

public interface IReviewService {
    ReviewResponseDto save(ReviewRequestDto request);
    List<ReviewResponseDto> findByStylistId(Long stylistId);
    List<ReviewResponseDto> findByClientId(Long clientId);
    Double averageRatingByStylistId(Long stylistId);
}
