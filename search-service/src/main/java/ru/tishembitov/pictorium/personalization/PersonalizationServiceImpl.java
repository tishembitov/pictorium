package ru.tishembitov.pictorium.personalization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.document.UserInterestDocument;
import ru.tishembitov.pictorium.repository.UserInterestRepository;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationServiceImpl implements PersonalizationService {

    private final UserInterestRepository interestRepository;

    private static final double LIKE_WEIGHT = 1.0;
    private static final double SAVE_WEIGHT = 2.0;
    private static final double VIEW_WEIGHT = 0.1;
    private static final double DECAY_FACTOR = 0.95;
    private static final int MAX_TAGS = 50;
    private static final int MAX_AUTHORS = 100;

    @Override
    @Async
    public void onPinLiked(String userId, String pinId, Set<String> tags, String authorId) {
        updateInterests(userId, tags, LIKE_WEIGHT, authorId);
        log.debug("User interests updated on like: userId={}, tags={}", userId, tags);
    }

    @Override
    @Async
    public void onPinSaved(String userId, String pinId, Set<String> tags, String authorId) {
        updateInterests(userId, tags, SAVE_WEIGHT, authorId);
        log.debug("User interests updated on save: userId={}, tags={}", userId, tags);
    }

    @Override
    @Async
    public void onPinViewed(String userId, String pinId, Set<String> tags, String authorId) {
        updateInterests(userId, tags, VIEW_WEIGHT, authorId);
    }

    @Override
    @Async
    public void onUserFollowed(String followerId, String followedId) {
        UserInterestDocument interests = getOrCreateInterests(followerId);
        interests.getFollowedAuthors().add(followedId);
        interests.setUpdatedAt(Instant.now());
        interestRepository.save(interests);
        log.debug("Added followed author: followerId={}, followedId={}", followerId, followedId);
    }

    @Override
    @Async
    public void onUserUnfollowed(String followerId, String unfollowedId) {
        interestRepository.findById(followerId).ifPresent(interests -> {
            interests.getFollowedAuthors().remove(unfollowedId);
            interests.setUpdatedAt(Instant.now());
            interestRepository.save(interests);
            log.debug("Removed followed author: followerId={}, unfollowedId={}", followerId, unfollowedId);
        });
    }

    @Override
    public Optional<UserInterestDocument> getUserInterests(String userId) {
        return interestRepository.findById(userId);
    }

    @Override
    public PersonalizationBoosts getBoostsForUser(String userId) {
        return interestRepository.findById(userId)
                .map(this::buildBoosts)
                .orElse(PersonalizationBoosts.empty());
    }


    private void updateInterests(String userId, Set<String> tags, double weight, String authorId) {
        if (userId == null || (tags == null && authorId == null)) {
            return;
        }

        UserInterestDocument interests = getOrCreateInterests(userId);

        if (tags != null) {
            Map<String, Double> tagWeights = interests.getTagWeights();

            tagWeights.replaceAll((k, v) -> v * DECAY_FACTOR);

            for (String tag : tags) {
                String normalizedTag = tag.toLowerCase();
                tagWeights.merge(normalizedTag, weight, Double::sum);
            }

            if (tagWeights.size() > MAX_TAGS) {
                Map<String, Double> trimmed = tagWeights.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(MAX_TAGS)
                        .collect(LinkedHashMap::new,
                                (m, e) -> m.put(e.getKey(), e.getValue()),
                                LinkedHashMap::putAll);
                interests.setTagWeights(trimmed);
            }
        }

        if (authorId != null && !authorId.equals(userId)) {
            Map<String, Integer> likedAuthors = interests.getLikedAuthors();
            likedAuthors.merge(authorId, 1, Integer::sum);

            if (likedAuthors.size() > MAX_AUTHORS) {
                Map<String, Integer> trimmed = likedAuthors.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(MAX_AUTHORS)
                        .collect(LinkedHashMap::new,
                                (m, e) -> m.put(e.getKey(), e.getValue()),
                                LinkedHashMap::putAll);
                interests.setLikedAuthors(trimmed);
            }
        }

        interests.setUpdatedAt(Instant.now());
        interestRepository.save(interests);
    }

    private UserInterestDocument getOrCreateInterests(String userId) {
        return interestRepository.findById(userId)
                .orElse(UserInterestDocument.builder()
                        .userId(userId)
                        .tagWeights(new HashMap<>())
                        .followedAuthors(new HashSet<>())
                        .likedAuthors(new HashMap<>())
                        .updatedAt(Instant.now())
                        .build());
    }

    private PersonalizationBoosts buildBoosts(UserInterestDocument interests) {
        Map<String, Float> tagBoosts = new LinkedHashMap<>();
        double maxTagWeight = interests.getTagWeights().values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);

        interests.getTagWeights().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> tagBoosts.put(e.getKey(),
                        (float) (e.getValue() / maxTagWeight * 2.0))); // max boost = 2.0

        Map<String, Float> authorBoosts = new LinkedHashMap<>();

        for (String authorId : interests.getFollowedAuthors()) {
            authorBoosts.put(authorId, 3.0f);
        }
        interests.getLikedAuthors().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> authorBoosts.merge(e.getKey(),
                        Math.min(e.getValue() * 0.5f, 2.0f), Float::sum));

        return PersonalizationBoosts.builder()
                .tagBoosts(tagBoosts)
                .authorBoosts(authorBoosts)
                .build();
    }
}