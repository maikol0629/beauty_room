package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Review;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends CrudRepository<Review, Long> {
    List<Review> findByStylistIdAndTenantId(Long stylistId, Long tenantId);
    List<Review> findByClientIdAndTenantId(Long clientId, Long tenantId);
    List<Review> findByTenantId(Long tenantId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.stylist.id = :stylistId AND r.tenant.id = :tenantId")
    Double findAverageRatingByStylistId(@Param("stylistId") Long stylistId, @Param("tenantId") Long tenantId);
}
