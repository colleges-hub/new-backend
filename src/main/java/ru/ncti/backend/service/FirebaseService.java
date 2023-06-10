package ru.ncti.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.entity.Chat;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.ChatRepository;
import ru.ncti.backend.repository.UserRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.ncti.backend.dto.RabbitQueue.CHAT_NOTIFICATION;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 05-06-2023
 */
@Service
@Log4j
@RequiredArgsConstructor
public class FirebaseService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final FirebaseMessaging firebaseMessaging;

    @RabbitListener(queues = CHAT_NOTIFICATION)
    @Transactional(readOnly = true)
    public void sendNotification(Map<String, String> map) throws FirebaseMessagingException {
        Chat chat = chatRepository.findById(UUID.fromString(map.get("chat")))
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        User user = userRepository.findByEmail(map.get("user"))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<User> usersInChat = chat.getUsers();
        Set<User> users = new HashSet<>(usersInChat);

        Set<String> onlineUserEmails = redisService.getValueSet(map.get("chat"));
        Set<User> userOnline = onlineUserEmails.stream()
                .map(userRepository::findByEmail)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        userOnline.add(user);

        // Users offline
        users.removeAll(userOnline);

        Set<String> usernames = users.stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());

        Set<String> fcmTokens = new HashSet<>();

        for (String username : usernames) {
            Set<String> deviceTokens = redisService.getValueSet(String.format("device:%s", username));
            if (deviceTokens != null) {
                fcmTokens.addAll(deviceTokens);
            }
        }
        
        if (!fcmTokens.isEmpty()) {
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .addAllTokens(fcmTokens)
                    .setNotification(Notification.builder()
                            .setTitle(String.format("Чат: %s", chat.getName()))
                            .setBody(String.format("%s: %s", user.getFirstname(), map.get("text")))
                            .build())
                    .putData("page", "ChatRoute")
                    .build();
            firebaseMessaging.sendMulticast(multicastMessage);
        }
    }

}
