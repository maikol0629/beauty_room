package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.review.ReviewRequestDto;
import com.mr.sb.beauty_room.DTOS.review.ReviewResponseDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IReviewService;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Review;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.ReviewRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ReviewServiceImplement implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public ReviewResponseDto save(ReviewRequestDto request) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Client client = clientRepository.findByIdAndTenantId(request.getClientId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Stylist stylist = stylistRepository.findByIdAndTenantId(request.getStylistId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Estilista no encontrado"));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .filter(a -> a.getTenant() != null && a.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
        }

        Review review = Review.builder()
                .client(client)
                .stylist(stylist)
                .appointment(appointment)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();

        return toDto(reviewRepository.save(review));
    }

    @Override
    public List<ReviewResponseDto> findByStylistId(Long stylistId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return reviewRepository.findByStylistIdAndTenantId(stylistId, tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<ReviewResponseDto> findByClientId(Long clientId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return reviewRepository.findByClientIdAndTenantId(clientId, tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Double averageRatingByStylistId(Long stylistId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<Review> reviews = reviewRepository.findByStylistIdAndTenantId(stylistId, tenantId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    private ReviewResponseDto toDto(Review review) {
        return ReviewResponseDto.builder()
                .id(review.getId())
                .clientId(review.getClient().getId())
                .clientName(review.getClient().getName_client())
                .stylistId(review.getStylist().getId())
                .stylistName(review.getStylist().getName_stylist())
                .appointmentId(review.getAppointment() != null ? review.getAppointment().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
