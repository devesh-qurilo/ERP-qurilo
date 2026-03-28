package com.erp.project_service.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discussion_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private DiscussionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private DiscussionMessage parentMessage;

    @OneToMany(mappedBy = "parentMessage", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DiscussionMessage> replies = new ArrayList<>();

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "deleted_by")
    private String deletedBy;

    // NEW FIELD: Mark as best reply
    @Column(name = "is_best_reply")
    private Boolean isBestReply;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
        if (messageType == null) messageType = MessageType.TEXT;
        if (isBestReply == null) isBestReply = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT, FILE, SYSTEM
    }
}