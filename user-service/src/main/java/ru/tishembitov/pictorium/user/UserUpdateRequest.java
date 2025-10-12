package ru.tishembitov.pictorium.user;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    String username,
    @Size(max = 200, message = "Description must not exceed 200 characters")
    String description,
    
    @Size(max = 100, message = "Instagram link must not exceed 100 characters")
    String instagram,
    
    @Size(max = 100, message = "TikTok link must not exceed 100 characters")
    String tiktok,
    
    @Size(max = 100, message = "Telegram link must not exceed 100 characters")
    String telegram,
    
    @Size(max = 100, message = "Pinterest link must not exceed 100 characters")
    String pinterest
) {
}