package com.mediconnect.repository;
import com.mediconnect.entity.Appointment;import com.mediconnect.enums.AppointmentStatus;import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;import java.util.UUID;
public interface AppointmentRepository extends JpaRepository<Appointment,UUID>{List<Appointment> findByPatient_IdOrderByCreatedAtDesc(UUID id);List<Appointment> findByDoctor_IdOrderByCreatedAtDesc(UUID id);long countByStatus(AppointmentStatus status);}
