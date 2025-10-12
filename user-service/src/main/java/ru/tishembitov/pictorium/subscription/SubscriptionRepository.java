package ru.tishembitov.pictorium.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tishembitov.pictorium.user.User;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    @Query("SELECT s.follower FROM Subscription s WHERE s.following.id = :userId")
    Page<User> findFollowersByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT s.following FROM Subscription s WHERE s.follower.id = :userId")
    Page<User> findFollowingByUserId(@Param("userId") String userId, Pageable pageable);

    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);
}