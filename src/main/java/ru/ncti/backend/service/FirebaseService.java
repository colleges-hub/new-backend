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

import static ru.ncti.backend.config.RabbitConfig.CERTIFICATE_NOTIFICATION;
import static ru.ncti.backend.config.RabbitConfig.CHANGE_SCHEDULE;
import static ru.ncti.backend.config.RabbitConfig.PRIVATE_CHAT_NOTIFICATION;
import static ru.ncti.backend.config.RabbitConfig.PUBLIC_CHAT_NOTIFICATION;
import static ru.ncti.backend.config.RabbitConfig.UPDATE_CLASSROOM;


/**
 * user: ichuvilin
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseService {
    
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
                    userOff.getDevice().forEach(device -> fcmTokens.add(device.getToken()));
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
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user2 = chat.getChatName(user);

        String chatWithUser = redisService.getValue(String.format("user:%s", user2.getUsername()));

        if (chatWithUser == null || !chatWithUser.equals(map.get("chat"))) {
            Set<String> fcmToken = user2.getDevice().stream().map(FCM::getToken).collect(Collectors.toSet());
            String name = String.format("%s %s", user.getFirstname(), user.getLastname());
            if (fcmToken.size() > 0) {
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
    }

    @Async
    @RabbitListener(queues = UPDATE_CLASSROOM)
    public void sendNotificationAboutChangeClassroom(Map<String, String> map) throws FirebaseMessagingException {
        Group group = groupRepository.findByName(map.get("group"))
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<User> students = userRepository.findAllByGroupOrderByLastname(group);
        Set<String> tokens = students.parallelStream()
                .flatMap(student -> student.getDevice().stream().map(FCM::getToken))
                .collect(Collectors.toSet());

        if (tokens.size() > 0) {
            String title = String.format("Преподаватель изменил кабинет на %s", map.get("date")
                    .split(" ")[0]);
            String body = String.format("№ пары %s : %s", map.get("pair"), map.get("classroom"));
            sendNotification(tokens, title, body);
        }
    }

    @Async
    @Transactional(readOnly = true)
    @RabbitListener(queues = CHANGE_SCHEDULE)
    public void notificationChangeSchedule(Map<String, String> map) throws FirebaseMessagingException {
        Group group = groupRepository.findById(Long.valueOf(map.get("group")))
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<User> students = userRepository.findAllByGroupOrderByLastname(group);
        Set<String> tokens = students.parallelStream()
                .flatMap(student -> student.getDevice().stream().map(FCM::getToken))
                .collect(Collectors.toSet());

        if (tokens.size() != 0) {
            String title = "Изменения в расписании";
            String body = String.format("Расписание на %s было изменено", map.get("day"));
            sendNotification(tokens, title, body);
        }
    }

    @Async
    @RabbitListener(queues = CERTIFICATE_NOTIFICATION)
    public void notificationUsers(List<String> emails) throws FirebaseMessagingException {
        for (String email : emails) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("User %s not found", email)));
            if (!user.getDevice().isEmpty()) {
                Set<String> tokens = user.getDevice().stream().map(FCM::getToken).collect(Collectors.toSet());
                String title = "Система";
                String body = "Ваша справка готова. Можете подойти и забрать ее.";
                sendNotification(tokens, title, body);
            }
        }
    }


    private synchronized void sendNotification(Set<String> tokens, String title, String body) throws FirebaseMessagingException {
        MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
        firebaseMessaging.sendMulticast(multicastMessage);
    }

}
