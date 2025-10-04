package ru.tishembitov.pictorium.user;

import java.util.UUID;

public record UserResponseDto(

     UUID id,
     String keycloakId,
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
