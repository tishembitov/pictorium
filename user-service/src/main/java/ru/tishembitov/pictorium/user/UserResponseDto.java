package ru.tishembitov.pictorium.user;

public record UserResponseDto(

     Long id,
     String keycloakId,
     String username,
     String image,
     String bannerImage,
     String description,
     String instagram,
     String tiktok,
     String telegram,
     String pinterest,
     String chatColor,
     Integer chatSize,
     Boolean sideOpen,
     Long selectedBoard
) {
}
