package ru.ncti.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.api.response.ViewChatResponse;
import ru.ncti.backend.model.FCM;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.PrivateChat;
import ru.ncti.backend.model.PublicChat;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.PrivateChatRepository;
import ru.ncti.backend.repository.PublicChatRepository;
import ru.ncti.backend.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.ncti.backend.config.RabbitConfig.PRIVATE_CHAT_NOTIFICATION;
import static ru.ncti.backend.config.RabbitConfig.PUBLIC_CHAT_NOTIFICATION;
import static ru.ncti.backend.config.RabbitConfig.UPDATE_SCHEDULE;


/**
 * user: ichuvilin
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseService {

    // TODO: change all

    private final PublicChatRepository publicChatRepository;
    private final UserRepository userRepository;
    private final PrivateChatRepository privateChatRepository;
    private final RedisService redisService;
    private final FirebaseMessaging firebaseMessaging;
    private final ObjectMapper objectMapper;
    private final GroupRepository groupRepository;

    @Async
    @RabbitListener(queues = PUBLIC_CHAT_NOTIFICATION)
    @Transactional(readOnly = true)
    public void sendPublicNotification(Map<String, String> map) throws FirebaseMessagingException, JsonProcessingException {
        PublicChat publicChat = publicChatRepository.findById(UUID.fromString(map.get("chat")))
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        User user = userRepository.findByEmail(map.get("user"))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<User> usersInChat = publicChat.getUsers();

        Set<String> onlineUserEmails = redisService.getValueSet(map.get("chat"));
        Set<User> userOnline = onlineUserEmails.stream()
                .map(userRepository::findByEmail)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (userOnline.size() != usersInChat.size()) {
            // Users offline
            Set<User> userOffline = new HashSet<>(usersInChat);
            userOffline.removeAll(userOnline);

            Set<String> fcmTokens = new HashSet<>();

            for (User userOff : userOffline) {
                if (userOff.getDevice() != null) {
                    user.getDevice().forEach(device -> fcmTokens.add(device.getToken()));
                }
            }

            if (!fcmTokens.isEmpty()) {
                MulticastMessage multicastMessage = MulticastMessage.builder()
                        .addAllTokens(fcmTokens)
                        .setNotification(Notification.builder()
                                .setTitle(String.format("Чат: %s", publicChat.getName()))
                                .setBody(String.format("%s: %s", user.getFirstname(), map.get("text")))
                                .build())
                        .putData("page", "ChatRoute")
                        .putData("chat", objectMapper.writeValueAsString(ViewChatResponse.builder()
                                .type("PUBLIC")
                                .name(publicChat.getName())
                                .id(publicChat.getId())
                                .build()))
                        .build();
                firebaseMessaging.sendMulticast(multicastMessage);
            }
        }
    }

    @Async
    @RabbitListener(queues = PRIVATE_CHAT_NOTIFICATION)
    @Transactional(readOnly = true)
    public void sendPrivateNotification(Map<String, String> map) throws FirebaseMessagingException, JsonProcessingException {
        User user = userRepository.findByEmail(map.get("user"))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PrivateChat chat = privateChatRepository.findById(UUID.fromString(map.get("chat")))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User user2 = chat.getChatName(user);

        String chatWithUser = redisService.getValue(String.format("user:%s", user2.getUsername()));

        if (chatWithUser == null || !chatWithUser.equals(map.get("chat"))) {
            Set<String> fcmToken = user2.getDevice().stream().map(FCM::getToken).collect(Collectors.toSet());
            String name = String.format("%s %s", user.getFirstname(), user.getLastname());

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(name)
                            .setBody(String.format("%s", map.get("text")))
                            .build())
                    .putData("page", "ChatRoute")
                    .putData("chat", objectMapper.writeValueAsString(ViewChatResponse.builder()
                            .type("PRIVATE")
                            .name(name)
                            .id(chat.getId())
                            .build()))
                    .build();
            firebaseMessaging.sendMulticast(message);
        }
    }

    @Async
    @RabbitListener(queues = UPDATE_SCHEDULE)
    public void sendNotificationAboutChanges(Map<String, ?> map) throws FirebaseMessagingException {
        Group group = groupRepository.findByName(map.get("group").toString())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<User> students = userRepository.findAllByGroupOrderByLastname(group);

        Set<String> tokens = new HashSet<>();

        students.forEach(student -> {
            student.getDevice().forEach(device -> tokens.add(device.getDevice()));
        });


        MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(String.format("Преподаватель изменил кабинет на %s", map.get("date").toString().split(" ")[0]))
                        .setBody(String.format("№ пары %d : %s", map.get("pair"), map.get("classroom")))
                        .build())
                .build();
        firebaseMessaging.sendMulticast(multicastMessage);
    }

}
