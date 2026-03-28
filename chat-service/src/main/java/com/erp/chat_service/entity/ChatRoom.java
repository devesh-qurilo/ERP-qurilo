//package com.erp.chat_service.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "chat_rooms")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class ChatRoom {
//
//    @Id
//    private String id;
//
//    @Column(name = "participant1_id", nullable = false)
//    private String participant1Id;
//
//    @Column(name = "participant2_id", nullable = false)
//    private String participant2Id;
//
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    private LocalDateTime updatedAt;
//}

package com.erp.chat_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    private static final ZoneId EET_ZONE = ZoneId.of("Europe/Helsinki"); // Eastern European Time

    @Id
    private String id;

    @Column(name = "participant1_id", nullable = false)
    private String participant1Id;

    @Column(name = "participant2_id", nullable = false)
    private String participant2Id;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        ZonedDateTime now = ZonedDateTime.now(EET_ZONE);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = ZonedDateTime.now(EET_ZONE);
    }
}
