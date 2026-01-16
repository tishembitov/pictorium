package ru.tishembitov.pictorium.user;

import ru.tishembitov.pictorium.kafka.event.UserEvent;

import java.util.Optional;

public interface UserIndexService {

    void indexUser(UserEvent event);

    void updateUser(UserEvent event);

    void updateUserCounters(UserEvent event);

    void deleteUser(String userId);

    Optional<UserDocument> findById(String userId);

    long count();
}