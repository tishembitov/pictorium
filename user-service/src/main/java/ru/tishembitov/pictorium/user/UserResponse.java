package ru.tishembitov.pictorium.user;

import java.util.UUID;

public record UserResponse(

     String id,
     String username,
     String email,
     String image,
     String bannerImage,
     String description,
     String instagram,
     String tiktok,
     String telegram,
     String pinterest,
     UUID selectedBoardId
) {
}
