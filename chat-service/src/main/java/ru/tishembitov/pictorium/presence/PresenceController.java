package ru.tishembitov.pictorium.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping
    public ResponseEntity<UserPresenceResponse> getOnlineStatus(
            @RequestParam Set<String> userIds
    ) {
        return ResponseEntity.ok(
                new UserPresenceResponse(presenceService.getOnlineStatus(userIds))
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable String userId) {
        return ResponseEntity.ok(presenceService.isUserOnline(userId));
    }
}
