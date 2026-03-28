package com.erp.lead_service.service;

import com.erp.lead_service.client.NotificationClient;
import com.erp.lead_service.dto.dto.notification.SendNotificationManyDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealFollowUp;
import com.erp.lead_service.entity.RemindUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerServiceImpl implements ReminderSchedulerService {

    // thread pool for scheduled tasks
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    // keep track of scheduled futures so we can cancel them later
    private final Map<Long, ScheduledFuture<?>> scheduledMap = new ConcurrentHashMap<>();

    private final NotificationClient notificationClient;

    @Override
    public void scheduleFollowupReminder(DealFollowUp followup, String authHeader) {
        if (followup == null || followup.getNextDate() == null) {
            log.debug("Followup or nextDate is null. Skipping schedule.");
            return;
        }

        if (followup.getRemindBefore() == null || followup.getRemindUnit() == null) {
            log.debug("Followup {} does not have remindBefore/remindUnit configured. Skipping schedule.", followup.getId());
            return;
        }

        // calculate followup datetime from nextDate + startTime (or default 09:00)
        LocalDate date = followup.getNextDate();
        LocalTime time = parseTimeOrDefault(followup.getStartTime(), LocalTime.of(9, 0));
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime followupDateTime = LocalDateTime.of(date, time);
        ZonedDateTime followupZdt = ZonedDateTime.of(followupDateTime, zone);

        // subtract remindBefore with unit
        ZonedDateTime triggerAt;
        try {
            RemindUnit unit = followup.getRemindUnit();
            int amount = followup.getRemindBefore() != null ? followup.getRemindBefore() : 0;
            switch (unit) {
                case DAYS:
                    triggerAt = followupZdt.minusDays(amount);
                    break;
                case HOURS:
                    triggerAt = followupZdt.minusHours(amount);
                    break;
                case MINUTES:
                    triggerAt = followupZdt.minusMinutes(amount);
                    break;
                default:
                    log.warn("Unknown remind unit for followup {}: {}. Scheduling at followup time.", followup.getId(), unit);
                    triggerAt = followupZdt;
            }
        } catch (Exception e) {
            log.warn("Failed to compute trigger time for followup {}: {}. Scheduling at followup time.", followup.getId(), e.getMessage());
            triggerAt = followupZdt;
        }

        long delayMs = Duration.between(ZonedDateTime.now(zone), triggerAt).toMillis();
        if (delayMs < 0) {
            log.info("Computed reminder time for followup {} is in the past; scheduling immediate execution.", followup.getId());
            delayMs = 0;
        }

        final String token = (authHeader != null) ? authHeader : "";

        // If already scheduled, cancel existing one first
        cancelScheduledFollowup(followup);

        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                Deal deal = followup.getDeal();
                if (deal == null) {
                    log.warn("Followup {} has no deal attached. Skipping reminder.", followup.getId());
                    return;
                }

                // collect watcher ids (defensive)
                List<String> watcherIds = new ArrayList<>();
                if (deal.getDealWatchers() != null) {
                    for (Object w : deal.getDealWatchers()) {
                        if (w != null) {
                            String id = w.toString().trim();
                            if (!id.isEmpty()) watcherIds.add(id);
                        }
                    }
                }

                if (watcherIds.isEmpty()) {
                    log.info("Deal {} has no watchers; no reminder sent for followup {}", deal.getId(), followup.getId());
                    return;
                }

                String title = "Deal Follow-up Reminder";
                String message = (followup.getRemarks() != null && !followup.getRemarks().isBlank())
                        ? followup.getRemarks()
                        : "Follow-up scheduled.";
                String type = "FOLLOWUP_REMINDER";

                SendNotificationManyDto many = new SendNotificationManyDto();
                many.setReceiverEmployeeIds(watcherIds);
                many.setTitle(title);
                many.setMessage(message + " (Deal ID: " + deal.getId() + ")");
                many.setType(type);

                // call employee-service via Feign client; pass token (may be empty)
                try {
                    notificationClient.sendMany(many, token);
                    log.info("Followup reminder sent to {} watcher(s) for deal {} (followup {}).",
                            watcherIds.size(), deal.getId(), followup.getId());
                } catch (Exception e) {
                    log.error("Failed to call notification client for followup {}: {}", followup.getId(), e.getMessage(), e);
                }

            } catch (Exception e) {
                log.error("Error executing followup reminder for id {}: {}", followup.getId(), e.getMessage(), e);
            } finally {
                // remove from scheduled map after run
                scheduledMap.remove(followup.getId());
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        // store scheduled future
        scheduledMap.put(followup.getId(), future);
        log.info("Scheduled reminder for followup {} at {} (in {} ms)", followup.getId(), triggerAt, delayMs);
    }

    @Override
    public void cancelScheduledFollowup(DealFollowUp followup) {
        if (followup == null || followup.getId() == null) return;
        ScheduledFuture<?> f = scheduledMap.remove(followup.getId());
        if (f != null) {
            boolean cancelled = f.cancel(false); // do not interrupt if running
            log.info("Cancelled scheduled reminder for followup {} : {}", followup.getId(), cancelled);
        } else {
            log.debug("No scheduled reminder found to cancel for followup {}", followup.getId());
        }
    }

    private LocalTime parseTimeOrDefault(String hhmm, LocalTime def) {
        if (hhmm == null || hhmm.isBlank()) return def;
        try {
            return LocalTime.parse(hhmm);
        } catch (DateTimeParseException e) {
            log.warn("Invalid startTime '{}' for followup; using default {}", hhmm, def);
            return def;
        }
    }
}
