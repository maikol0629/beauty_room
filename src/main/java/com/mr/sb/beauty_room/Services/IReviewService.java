package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.review.ReviewRequestDto;
import com.mr.sb.beauty_room.DTOS.review.ReviewResponseDto;

import java.util.List;

public interface IReviewService {
    ReviewResponseDto save(ReviewRequestDto request);
    List<ReviewResponseDto> findByStylistId(Long stylistId);
    List<ReviewResponseDto> findByClientId(Long clientId);
    Double averageRatingByStylistId(Long stylistId);
}
