package com.erp.project_service.service;

import com.erp.project_service.entity.Task;
import com.erp.project_service.entity.TaskUserState;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.repository.TaskUserStateRepository;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

//@Service
//@RequiredArgsConstructor
//public class TaskUserStateService {
//    private final TaskUserStateRepository repo;
//    private final TaskRepository taskRepo;
//
//    private boolean isAdmin(Authentication auth) {
//        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
//    }
//
//    private Task ensureVisible(Long taskId, String actor, boolean admin) {
//        Task t = taskRepo.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
//        if (!admin) {
//            // employee must be assigned to this task OR at least to the project (keeping it strict to task)
//            boolean assigned = taskRepo.isEmployeeAssignedToTask(actor, taskId);
//            if (!assigned) throw new IllegalArgumentException("Not allowed");
//        }
//        return t;
//    }
//
//    private TaskUserState getOrCreate(Long taskId, String userId) {
//        return repo.findByTaskIdAndUserId(taskId, userId)
//                .orElse(TaskUserState.builder().taskId(taskId).userId(userId).build());
//    }
//
//    public void pin(Long taskId, String actor, Authentication auth) {
//        ensureVisible(taskId, actor, isAdmin(auth));
//        TaskUserState s = getOrCreate(taskId, actor);
//        if (s.getPinnedAt() == null) s.setPinnedAt(Instant.now());
//        repo.save(s);
//    }
//
//    public void unpin(Long taskId, String actor, Authentication auth) {
//        ensureVisible(taskId, actor, isAdmin(auth));
//        TaskUserState s = getOrCreate(taskId, actor);
//        if (s.getPinnedAt() != null) s.setPinnedAt(null);
//        repo.save(s);
//    }
//
//    public List<TaskUserState> listPinned(String actor) {
//        return repo.findByUserIdAndPinnedAtNotNullOrderByPinnedAtDesc(actor);
//    }
//}

// TaskUserStateService.java
//@Service
//@RequiredArgsConstructor
//public class TaskUserStateService {
//    private final TaskUserStateRepository repo;
//    private final TaskRepository taskRepo;
//
//    // use SecurityUtils centrally
//    private boolean isAdmin() {
//        return SecurityUtils.isAdmin();
//    }
//
//    private Task ensureVisible(Long taskId, String actor, boolean admin) {
//        Task t = taskRepo.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
//        if (!admin) {
//            // employee must be assigned to this task (strict)
//            boolean assigned = taskRepo.isEmployeeAssignedToTask(actor, taskId);
//            if (!assigned) throw new IllegalArgumentException("Not allowed");
//        }
//        return t;
//    }
//
//    private TaskUserState getOrCreate(Long taskId, String userId) {
//        return repo.findByTaskIdAndUserId(taskId, userId)
//                .orElse(TaskUserState.builder().taskId(taskId).userId(userId).build());
//    }
//
//    public void pin(Long taskId) {
//        String actor = SecurityUtils.getCurrentUserId();
//        boolean admin = isAdmin();
//        ensureVisible(taskId, actor, admin);
//        TaskUserState s = getOrCreate(taskId, actor);
//        if (s.getPinnedAt() == null) s.setPinnedAt(Instant.now());
//        repo.save(s);
//    }
//
//    public void unpin(Long taskId) {
//        String actor = SecurityUtils.getCurrentUserId();
//        boolean admin = isAdmin();
//        ensureVisible(taskId, actor, admin);
//        TaskUserState s = getOrCreate(taskId, actor);
//        if (s.getPinnedAt() != null) s.setPinnedAt(null);
//        repo.save(s);
//    }
//
//    public List<TaskUserState> listPinned(String actor) {
//        return repo.findByUserIdAndPinnedAtNotNullOrderByPinnedAtDesc(actor);
//    }
//}



import com.erp.project_service.entity.Task;
import com.erp.project_service.entity.TaskUserState;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.repository.TaskUserStateRepository;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskUserStateService {

    private final TaskUserStateRepository repo;
    private final TaskRepository taskRepo;

    /**
     * Ensure task exists. Do not enforce assignment/admin here — caller decides.
     */
    private Task ensureExists(Long taskId) {
        return taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    private TaskUserState getOrCreate(Long taskId, String userId) {
        return repo.findByTaskIdAndUserId(taskId, userId)
                .orElse(TaskUserState.builder().taskId(taskId).userId(userId).build());
    }

    /**
     * Pin for the current user. Admins will simply pin for themselves (per-user).
     */
    public void pin(Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        boolean admin = SecurityUtils.isAdmin();
        log.info("PIN attempt by actor='{}' isAdmin={} for taskId={}", actor, admin, taskId);

        ensureExists(taskId);
        TaskUserState s = getOrCreate(taskId, actor);
        if (s.getPinnedAt() == null) {
            s.setPinnedAt(Instant.now());
            repo.save(s);
            log.info("Task {} pinned for user {}", taskId, actor);
        } else {
            log.info("Task {} already pinned for user {}", taskId, actor);
        }
    }

    /**
     * Unpin for current user.
     */
    public void unpin(Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        boolean admin = SecurityUtils.isAdmin();
        log.info("UNPIN attempt by actor='{}' isAdmin={} for taskId={}", actor, admin, taskId);

        ensureExists(taskId);
        TaskUserState s = getOrCreate(taskId, actor);
        if (s.getPinnedAt() != null) {
            s.setPinnedAt(null);
            repo.save(s);
            log.info("Task {} unpinned for user {}", taskId, actor);
        } else {
            log.info("Task {} was not pinned for user {}", taskId, actor);
        }
    }

    /**
     * Find TaskUserState row for given taskId and userId.
     * Returns null if not present.
     */
    public TaskUserState findByTaskIdAndUserId(Long taskId, String userId) {
        if (userId == null) return null;
        Optional<TaskUserState> opt = repo.findByTaskIdAndUserId(taskId, userId);
        return opt.orElse(null);
    }

    /**
     * Is pinned for the given user?
     */
    public boolean isPinnedForUser(Long taskId, String userId) {
        TaskUserState s = findByTaskIdAndUserId(taskId, userId);
        return s != null && s.getPinnedAt() != null;
    }

    /**
     * return pinnedAt for given user (may be null).
     */
    public Instant getPinnedAtForUser(Long taskId, String userId) {
        TaskUserState s = findByTaskIdAndUserId(taskId, userId);
        return s == null ? null : s.getPinnedAt();
    }

    /**
     * Admin (or an authorized caller) can pin for another user.
     * Caller should ensure only admins call this endpoint (controller enforces).
     */
    public void pinForUser(Long taskId, String targetUserId, String performedBy) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("targetUserId required");
        }
        ensureExists(taskId);
        TaskUserState s = repo.findByTaskIdAndUserId(taskId, targetUserId)
                .orElse(TaskUserState.builder().taskId(taskId).userId(targetUserId).build());

        if (s.getPinnedAt() == null) {
            s.setPinnedAt(Instant.now());
            repo.save(s);
        } else {
            repo.save(s); // no-op but keep last modified if needed
        }
        log.info("Admin/actor '{}' pinned task {} for target user {}", performedBy, taskId, targetUserId);
    }

    /**
     * Admin can unpin for another user.
     */
    public void unpinForUser(Long taskId, String targetUserId, String performedBy) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("targetUserId required");
        }
        ensureExists(taskId);
        TaskUserState s = repo.findByTaskIdAndUserId(taskId, targetUserId)
                .orElse(null);
        if (s != null && s.getPinnedAt() != null) {
            s.setPinnedAt(null);
            repo.save(s);
            log.info("Admin/actor '{}' unpinned task {} for target user {}", performedBy, taskId, targetUserId);
        }
    }

    public List<TaskUserState> listPinned(String actor) {
        return repo.findByUserIdAndPinnedAtNotNullOrderByPinnedAtDesc(actor);
    }
}
