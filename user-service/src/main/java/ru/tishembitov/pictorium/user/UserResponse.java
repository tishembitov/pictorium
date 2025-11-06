package ru.tishembitov.pictorium.user;

import java.util.UUID;

public record UserResponse(

     String id,
     String username,
     String email,
     String imageUrl,
     String bannerImageUrl,
     String description,
     String instagram,
     String tiktok,
     String telegram,
     String pinterest,
     UUID selectedBoardId
) {
}
