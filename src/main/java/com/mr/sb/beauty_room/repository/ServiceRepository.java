package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Service;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ServiceRepository extends CrudRepository<Service, Long> {


    @Query("SELECT s FROM Service s WHERE s.stylist.id  =: id")
    List<Service> findServicesStylistId(@Param("id") long id);

}
