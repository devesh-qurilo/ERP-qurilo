//package com.erp.employee_service.service.document;
//
//import com.erp.employee_service.entity.Employee;
//import com.erp.employee_service.entity.FileMeta;
//import com.erp.employee_service.exception.ResourceNotFoundException;
//import com.erp.employee_service.repository.EmployeeRepository;
//import com.erp.employee_service.repository.FileMetaRepository;
//import com.erp.employee_service.service.SupabaseStorageService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class DocumentService {
//
//    private final EmployeeRepository employeeRepo;
//    private final FileMetaRepository fileRepo;
//    private final SupabaseStorageService storageService;
//
//    @Transactional
//    public FileMeta uploadDocument(String employeeId, MultipartFile file, String uploadedBy) {
//        Employee employee = employeeRepo.findByEmployeeId(employeeId)
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
//
//        // upload to supabase (returns FileMeta with url/path etc.)
//        FileMeta meta = storageService.uploadFile(file, "employee-documents/" + employee.getEmployeeId(), uploadedBy);
//
//        // attach employee & entityType then persist
//        meta.setEmployee(employee);
//        meta.setEntityType("DOCUMENT");
//        return fileRepo.save(meta);
//    }
//
//    @Transactional(readOnly = true)
//    public List<FileMeta> getAllDocuments(String employeeId) {
//        Employee employee = employeeRepo.findByEmployeeId(employeeId)
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
//        return fileRepo.findByEmployeeAndEntityType(employee, "DOCUMENT");
//    }
//
//    @Transactional(readOnly = true)
//    public FileMeta getDocument(String employeeId, Long docId) {
//        Employee employee = employeeRepo.findByEmployeeId(employeeId)
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
//        return fileRepo.findByIdAndEmployee(docId, employee)
//                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + docId));
//    }
//
//    @Transactional
//    public void deleteDocument(String employeeId, Long docId) {
//        FileMeta meta = getDocument(employeeId, docId);
//
//        // Optionally: delete from supabase storage as well
//        try {
//            storageService.deleteFile(meta.getPath()); // implement deleteFile in SupabaseStorageService
//        } catch (Exception ex) {
//            // log, but still delete metadata (or decide otherwise)
//            System.err.println("Warning: supabase delete failed for " + meta.getPath() + " : " + ex.getMessage());
//        }
//        fileRepo.delete(meta);
//    }
//
//    @Transactional(readOnly = true)
//    public ResponseEntity<ByteArrayResource> downloadDocument(String employeeId, Long docId) {
//        FileMeta meta = getDocument(employeeId, docId);
//
//        byte[] fileBytes = storageService.downloadFile(meta.getPath());
//        ByteArrayResource resource = new ByteArrayResource(fileBytes);
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFilename() + "\"")
//                .contentType(MediaType.parseMediaType(meta.getMime() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.getMime()))
//                .contentLength(meta.getSize() == null ? fileBytes.length : meta.getSize())
//                .body(resource);
//    }
//}
package com.erp.employee_service.service.document;

import com.erp.employee_service.dto.notification.SendNotificationDto;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.FileMetaRepository;
import com.erp.employee_service.service.SupabaseStorageService;
import com.erp.employee_service.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final EmployeeRepository employeeRepo;
    private final FileMetaRepository fileRepo;
    private final SupabaseStorageService storageService;
    private final NotificationService notificationService;

    @Transactional
    public FileMeta uploadDocument(String employeeId, MultipartFile file, String uploadedBy) {
        Employee employee = employeeRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        FileMeta meta = storageService.uploadFile(file, "employee-documents/" + employee.getEmployeeId(), uploadedBy);
        meta.setEmployee(employee);
        meta.setEntityType("DOCUMENT");
        FileMeta saved = fileRepo.save(meta);

        // Send notification asynchronously
        sendDocumentUploadNotification(employee, meta, uploadedBy);

        return saved;
    }

    @Async
    public CompletableFuture<Void> sendDocumentUploadNotification(Employee employee, FileMeta meta, String uploadedBy) {
        try {
            SendNotificationDto dto = new SendNotificationDto();
            dto.setReceiverEmployeeId(employee.getEmployeeId());
            dto.setTitle("Document Uploaded");
            dto.setMessage("A new document \"" + meta.getFilename() + "\" has been uploaded to your profile.");
            dto.setType("DOCUMENT");

            notificationService.sendNotification(uploadedBy, dto);
            log.info("Document upload notification sent successfully to employee: {}", employee.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to send document upload notification: {}", e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public List<FileMeta> getAllDocuments(String employeeId) {
        Employee employee = employeeRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        return fileRepo.findByEmployeeAndEntityType(employee, "DOCUMENT");
    }

    @Transactional(readOnly = true)
    public FileMeta getDocument(String employeeId, Long docId) {
        Employee employee = employeeRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        return fileRepo.findByIdAndEmployee(docId, employee)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + docId));
    }

    @Transactional
    public void deleteDocument(String employeeId, Long docId) {
        FileMeta meta = getDocument(employeeId, docId);

        try {
            storageService.deleteFile(meta.getPath());
        } catch (Exception ex) {
            log.warn("Supabase delete failed for {} : {}", meta.getPath(), ex.getMessage());
        }
        fileRepo.delete(meta);

        // Send notification asynchronously
        sendDocumentDeleteNotification(employeeId, meta);
    }

    @Async
    public CompletableFuture<Void> sendDocumentDeleteNotification(String employeeId, FileMeta meta) {
        try {
            SendNotificationDto dto = new SendNotificationDto();
            dto.setReceiverEmployeeId(employeeId);
            dto.setTitle("Document Deleted");
            dto.setMessage("Your document \"" + meta.getFilename() + "\" has been deleted.");
            dto.setType("DOCUMENT");

            notificationService.sendNotification(null, dto);
            log.info("Document delete notification sent successfully to employee: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to send document delete notification: {}", e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> downloadDocument(String employeeId, Long docId) {
        FileMeta meta = getDocument(employeeId, docId);

        byte[] fileBytes = storageService.downloadFile(meta.getPath());
        ByteArrayResource resource = new ByteArrayResource(fileBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(meta.getMime() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.getMime()))
                .contentLength(meta.getSize() == null ? fileBytes.length : meta.getSize())
                .body(resource);
    }
}