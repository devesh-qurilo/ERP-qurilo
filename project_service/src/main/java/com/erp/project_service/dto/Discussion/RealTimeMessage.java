package com.erp.project_service.dto.Discussion;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeMessage {
    private String type; // "NEW_MESSAGE", "UPDATE_MESSAGE", "DELETE_MESSAGE", "NEW_ROOM"
    private Long roomId;
    private DiscussionMessageResponse message;
    private DiscussionRoomResponse room;
    private String actionBy;
    private LocalDateTime timestamp;
}