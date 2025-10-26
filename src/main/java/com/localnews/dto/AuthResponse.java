package com.localnews.dto;

public class AuthResponse {

    private String token;
    private String message;
    private Long userId;
    private String mobileNumber;
    private String districtName;
    private boolean success;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(String token, Long userId, String mobileNumber, String districtName) {
        this.token = token;
        this.userId = userId;
        this.mobileNumber = mobileNumber;
        this.districtName = districtName;
        this.success = true;
        this.message = "Authentication successful";
    }

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
