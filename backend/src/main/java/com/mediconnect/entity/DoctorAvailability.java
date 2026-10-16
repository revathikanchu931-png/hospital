package com.mediconnect.entity;
import jakarta.persistence.*;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;
import java.time.LocalDate;import java.time.LocalTime;import java.util.UUID;
@Entity @Table(name="doctor_availability",uniqueConstraints=@UniqueConstraint(name="uk_doctor_slot",columnNames={"doctor_id","slot_date","start_time"}),indexes=@Index(name="idx_doctor_slot_date",columnList="doctor_id,slot_date,available"))
public class DoctorAvailability {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="doctor_id",nullable=false) private User doctor;
 @Column(name="slot_date",nullable=false) private LocalDate date;
 @Column(name="start_time",nullable=false) private LocalTime startTime;
 @Column(name="end_time",nullable=false) private LocalTime endTime;
 @Column(nullable=false) private boolean available=true;
 protected DoctorAvailability(){}
 public DoctorAvailability(User doctor,LocalDate date,LocalTime startTime,LocalTime endTime){this.doctor=doctor;this.date=date;this.startTime=startTime;this.endTime=endTime;}
 public UUID getId(){return id;} public User getDoctor(){return doctor;} public LocalDate getDate(){return date;} public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public boolean isAvailable(){return available;} public void setAvailable(boolean v){available=v;}
}
