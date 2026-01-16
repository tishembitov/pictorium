package ru.tishembitov.pictorium.personalization;

import ru.tishembitov.pictorium.document.UserInterestDocument;

import java.util.Optional;
import java.util.Set;

public interface PersonalizationService {

    void onPinLiked(String userId, String pinId, Set<String> tags, String authorId);

    void onPinSaved(String userId, String pinId, Set<String> tags, String authorId);

    void onPinViewed(String userId, String pinId, Set<String> tags, String authorId);

    void onUserFollowed(String followerId, String followedId);

    void onUserUnfollowed(String followerId, String unfollowedId);

    Optional<UserInterestDocument> getUserInterests(String userId);

    PersonalizationBoosts getBoostsForUser(String userId);
}