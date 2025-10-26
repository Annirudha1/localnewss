package com.localnews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SendOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number format")
    @Size(min = 10, max = 10, message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    // Constructors
    public SendOtpRequest() {}

    public SendOtpRequest(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    // Getters and setters
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
}
