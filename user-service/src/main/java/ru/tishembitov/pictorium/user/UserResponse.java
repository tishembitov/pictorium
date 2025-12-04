package ru.tishembitov.pictorium.user;

public record UserResponse(

     String id,
     String username,
     String email,
     String imageId,
     String bannerImageId,
     String description,
     String instagram,
     String tiktok,
     String telegram,
     String pinterest
) {
}
