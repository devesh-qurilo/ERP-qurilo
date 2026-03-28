//package com.erp.chat_service.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.HashSet;
//import java.util.Set;
//
//@Entity
//@Table(name = "chat_messages")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class ChatMessage {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "chat_room_id", nullable = false)
//    private String chatRoomId;
//
//    @Column(name = "sender_id", nullable = false)
//    private String senderId;
//
//    @Column(name = "receiver_id", nullable = false)
//    private String receiverId;
//
//    @Column(columnDefinition = "TEXT")
//    private String content;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "message_type")
//    private MessageType messageType;
//
//    @OneToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "file_attachment_id")
//    private FileAttachment fileAttachment;
//
//    @Enumerated(EnumType.STRING)
//    private MessageStatus status;
//
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//    @ElementCollection
//    @CollectionTable(name = "message_deleted_for_users", joinColumns = @JoinColumn(name = "message_id"))
//    @Column(name = "user_id")
//    private Set<String> deletedForUsers = new HashSet<>();
//
//}

package com.erp.chat_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private static final ZoneId EET_ZONE = ZoneId.of("Europe/Helsinki"); // Eastern European Time

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private String chatRoomId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "file_attachment_id")
    private FileAttachment fileAttachment;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE", updatable = false)
    private ZonedDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "message_deleted_for_users", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id")
    private Set<String> deletedForUsers = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = ZonedDateTime.now(EET_ZONE);
    }
}
