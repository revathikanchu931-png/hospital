package com.mediconnect.repository;
import com.mediconnect.entity.DoctorAvailability;import org.springframework.data.jpa.repository.JpaRepository;import java.time.LocalDate;import java.util.List;import java.util.UUID;
public interface AvailabilityRepository extends JpaRepository<DoctorAvailability,UUID>{List<DoctorAvailability> findByDoctor_IdAndDateGreaterThanEqualAndAvailableTrueOrderByDateAscStartTimeAsc(UUID doctorId,LocalDate date);}
