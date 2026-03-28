package com.erp.auth_service.dto;

// TranslateRequest.java
public class TranslateRequest {
    private String text;
    private String targetLang;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }
}
