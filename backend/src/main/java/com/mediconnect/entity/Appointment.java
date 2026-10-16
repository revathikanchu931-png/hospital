package com.mediconnect.entity;
import com.mediconnect.enums.AppointmentStatus;import jakarta.persistence.*;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="appointments",uniqueConstraints=@UniqueConstraint(name="uk_appointment_slot",columnNames="availability_id"),indexes=@Index(name="idx_appointments_patient",columnList="patient_id,status"))
public class Appointment{
 @Id @GeneratedValue(strategy=GenerationType.UUID) @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="patient_id",nullable=false) private User patient;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="doctor_id",nullable=false) private User doctor;
 @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="availability_id",nullable=false,unique=true) private DoctorAvailability availability;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AppointmentStatus status=AppointmentStatus.PENDING;
 @Column(nullable=false,name="created_at",updatable=false) private Instant createdAt=Instant.now();
 protected Appointment(){} public Appointment(User patient,User doctor,DoctorAvailability availability){this.patient=patient;this.doctor=doctor;this.availability=availability;}
 public UUID getId(){return id;} public User getPatient(){return patient;} public User getDoctor(){return doctor;} public DoctorAvailability getAvailability(){return availability;} public AppointmentStatus getStatus(){return status;} public void setStatus(AppointmentStatus s){status=s;} public Instant getCreatedAt(){return createdAt;}
}
