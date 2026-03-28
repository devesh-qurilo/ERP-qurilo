package com.erp.auth_service.service;

import com.erp.auth_service.entity.RefreshToken;
import com.erp.auth_service.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    public RefreshToken save(String token, String employeeId, LocalDateTime expiry) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setEmployeeId(employeeId);
        rt.setExpiry(expiry);
        System.out.println("Saving refresh token for " + employeeId + " with expiry " + expiry);
        return repo.save(rt);
    }

    public boolean isValid(String token) {
        return repo.findByTokenAndActiveTrue(token)
                .filter(rt -> rt.getExpiry().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    public void invalidate(String token) {
        repo.findByTokenAndActiveTrue(token).ifPresent(rt -> {
            rt.setActive(false);
            repo.save(rt);
        });
    }

    public Optional<RefreshToken> findValidToken(String token) {
        return repo.findByTokenAndActiveTrue(token)
                .filter(rt -> rt.getExpiry().isAfter(LocalDateTime.now()));
    }

}

