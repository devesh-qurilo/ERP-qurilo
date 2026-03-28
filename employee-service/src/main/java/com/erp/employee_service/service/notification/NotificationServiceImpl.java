package com.erp.employee_service.service.notification;

import com.erp.employee_service.dto.notification.NotificationDto;
import com.erp.employee_service.dto.notification.SendNotificationDto;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.notification.Notification;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.NotificationRepository;
import com.erp.employee_service.service.pushNotification.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;
    private final EmployeeRepository employeeRepo;
    private final PushService pushService;

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getById(Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return toDto(n);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getMyNotifications(String employeeId) {
        Employee emp = employeeRepo.findByEmployeeId(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return repo.findByReceiverOrderByCreatedAtDesc(emp).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void markRead(Long id, String employeeId) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getReceiver().getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("Not allowed");
        }
        n.setReadFlag(true);
        n.setReadAt(LocalDateTime.now());
        repo.save(n);
    }

    @Override
    public void markUnread(Long id, String employeeId) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getReceiver().getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("Not allowed");
        }
        n.setReadFlag(false);
        n.setReadAt(null);
        repo.save(n);
    }

    @Override
    public void clearAll(String employeeId) {
        Employee emp = employeeRepo.findByEmployeeId(employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        List<Notification> list = repo.findByReceiverOrderByCreatedAtDesc(emp);
        repo.deleteAll(list);
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> sendNotification(String senderEmployeeId, SendNotificationDto dto) {
        try {
            Employee sender = null;
            if (senderEmployeeId != null) {
                sender = employeeRepo.findByEmployeeId(senderEmployeeId).orElse(null);
            }
            Employee receiver = employeeRepo.findByEmployeeId(dto.getReceiverEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

            Notification n = new Notification();
            n.setSender(sender);
            n.setReceiver(receiver);
            n.setTitle(dto.getTitle());
            n.setMessage(dto.getMessage());
            n.setType(dto.getType());
            n.setReadFlag(false);
            n.setCreatedAt(LocalDateTime.now());

            repo.save(n);
            // after repo.save(n);
            try {
                Map<String,String> data = Map.of(
                        "module","employee",
                        "notificationId", String.valueOf(n.getId())
                );
                pushService.sendPushToUser(n.getReceiver().getEmployeeId(), n.getTitle(), n.getMessage(), data);
            } catch (Exception ex) {
                log.warn("Push send failed for notification {}: {}", n.getId(), ex.getMessage());
            }

            log.info("Notification sent successfully from {} to {}",
                    sender != null ? sender.getEmployeeId() : "system",
                    receiver.getEmployeeId());

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
            // Don't rethrow to avoid affecting the main transaction
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> sendNotificationMany(String senderEmployeeId, List<String> receiverEmployeeIds, String title, String message, String type) {
        try {
            Employee sender = null;
            if (senderEmployeeId != null) {
                sender = employeeRepo.findByEmployeeId(senderEmployeeId).orElse(null);
            }

            for (String rid : receiverEmployeeIds) {
                Employee receiver = employeeRepo.findByEmployeeId(rid)
                        .orElseThrow(() -> new ResourceNotFoundException("Receiver not found: " + rid));

                Notification n = new Notification();
                n.setSender(sender);
                n.setReceiver(receiver);
                n.setTitle(title);
                n.setMessage(message);
                n.setType(type);
                n.setReadFlag(false);
                n.setCreatedAt(LocalDateTime.now());

                repo.save(n);
                // after repo.save(n);
                try {
                    Map<String,String> data = Map.of(
                            "module","employee",
                            "notificationId", String.valueOf(n.getId())
                    );
                    pushService.sendPushToUser(n.getReceiver().getEmployeeId(), n.getTitle(), n.getMessage(), data);
                } catch (Exception ex) {
                    log.warn("Push send failed for notification {}: {}", n.getId(), ex.getMessage());
                }
            }

            log.info("Bulk notification sent successfully to {} employees", receiverEmployeeIds.size());

        } catch (Exception e) {
            log.error("Failed to send bulk notifications: {}", e.getMessage(), e);
            // Don't rethrow to avoid affecting the main transaction
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void deleteById(Long id, String employeeId) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        // allow delete if receiver or admin (admin check should be done by caller via roles; but we keep simple)
        if (!n.getReceiver().getEmployeeId().equals(employeeId)) {
            // for safety, throw (admin endpoints can call repository directly to delete)
            throw new AccessDeniedException("Not allowed");
        }
        repo.deleteById(id);
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto d = new NotificationDto();
        d.setId(n.getId());
        d.setSenderEmployeeId(n.getSender() != null ? n.getSender().getEmployeeId() : null);
        d.setReceiverEmployeeId(n.getReceiver().getEmployeeId());
        d.setTitle(n.getTitle());
        d.setMessage(n.getMessage());
        d.setType(n.getType());
        d.setReadFlag(n.isReadFlag());
        d.setCreatedAt(n.getCreatedAt());
        d.setReadAt(n.getReadAt());
        return d;
    }
}