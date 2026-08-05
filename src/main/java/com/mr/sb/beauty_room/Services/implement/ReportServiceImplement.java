package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.report.AppointmentReportDto;
import com.mr.sb.beauty_room.DTOS.report.RevenueReportDto;
import com.mr.sb.beauty_room.DTOS.report.TopServiceDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IReportService;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Payment;
import com.mr.sb.beauty_room.entities.Review;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.PaymentRepository;
import com.mr.sb.beauty_room.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ReportServiceImplement implements IReportService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public AppointmentReportDto getAppointmentReport(LocalDateTime from, LocalDateTime to) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<Appointment> appointments = appointmentRepository.findByTenantId(tenantId).stream()
                .filter(a -> !a.getStartDate().isBefore(from) && !a.getStartDate().isAfter(to))
                .toList();

        long total = appointments.size();
        long completed = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        long pending = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();
        long confirmed = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();

        return AppointmentReportDto.builder()
                .period(from.toLocalDate() + " - " + to.toLocalDate())
                .totalAppointments(total)
                .completed(completed)
                .cancelled(cancelled)
                .pending(pending)
                .confirmed(confirmed)
                .build();
    }

    @Override
    public RevenueReportDto getRevenueReport(LocalDateTime from, LocalDateTime to) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Double totalRevenue = paymentRepository.totalRevenueBetween(tenantId, from, to);
        List<Payment> payments = paymentRepository.findByTenantId(tenantId);
        long totalPayments = payments.stream()
                .filter(p -> p.getPaidAt() != null
                        && !p.getPaidAt().isBefore(from)
                        && !p.getPaidAt().isAfter(to))
                .count();

        return RevenueReportDto.builder()
                .period(from.toLocalDate() + " - " + to.toLocalDate())
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .totalPayments(totalPayments)
                .build();
    }

    @Override
    public List<TopServiceDto> getTopServices(LocalDateTime from, LocalDateTime to) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<Appointment> appointments = appointmentRepository.findByTenantId(tenantId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED
                        && !a.getStartDate().isBefore(from)
                        && !a.getStartDate().isAfter(to))
                .toList();

        return appointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getService().getName_service(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long count = list.size();
                                    double revenue = list.stream()
                                            .mapToDouble(a -> a.getService().getPrice())
                                            .sum();
                                    return TopServiceDto.builder()
                                            .serviceName(list.get(0).getService().getName_service())
                                            .totalBookings(count)
                                            .totalRevenue(revenue)
                                            .build();
                                }
                        )
                ))
                .values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalBookings(), a.getTotalBookings()))
                .toList();
    }

    @Override
    public long getActiveClients() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return clientRepository.findByTenantId(tenantId).size();
    }

    @Override
    public double getAverageRating() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<Review> allReviews = reviewRepository.findByTenantId(tenantId);
        if (allReviews.isEmpty()) {
            return 0.0;
        }
        return allReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
