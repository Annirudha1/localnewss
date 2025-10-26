package com.localnews.controller;

import com.localnews.config.JwtUtil;
import com.localnews.dto.UserDto;
import com.localnews.entity.User;
import com.localnews.service.UserService;
import com.localnews.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    // Mobile App Routes
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mobileLogin() {
        return "mobile-login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        try {
            // Clear session data
            if (session != null) {
                session.removeAttribute("user");
                session.invalidate();
            }

            // Redirect to login page (which is the OTP sending page)
            return "redirect:/login";
        } catch (Exception e) {
            // Even if there's an error, redirect to login
            return "redirect:/login";
        }
    }

    @GetMapping("/video-feed")
    public String videoFeed() {
        return "video-feed";
    }

    // Admin Routes
    @GetMapping("/admin")
    public String adminLogin() {
        return "admin-login";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/admin/videos")
    public String adminVideos() {
        return "admin-videos";
    }

    // Legacy routes for backward compatibility
    @GetMapping("/register")
    public String register() {
        return "redirect:/login";
    }

    @GetMapping("/media")
    public String media() {
        return "redirect:/video-feed";
    }

    @GetMapping("/upload")
    public String upload() {
        return "redirect:/admin/videos";
    }

    // API Health Check
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}
