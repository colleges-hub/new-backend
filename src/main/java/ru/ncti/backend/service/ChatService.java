package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.api.request.ChatRequest;
import ru.ncti.backend.api.request.MessageRequest;
import ru.ncti.backend.api.request.UsersRequest;
import ru.ncti.backend.api.response.MessageResponse;
import ru.ncti.backend.api.response.UserMessageResponse;
import ru.ncti.backend.api.response.ViewChatResponse;
import ru.ncti.backend.model.Message;
import ru.ncti.backend.model.PrivateChat;
import ru.ncti.backend.model.PublicChat;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.MessageRepository;
import ru.ncti.backend.repository.PrivateChatRepository;
import ru.ncti.backend.repository.PublicChatRepository;
import ru.ncti.backend.repository.UserRepository;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ru.ncti.backend.config.RabbitConfig.PRIVATE_CHAT_NOTIFICATION;
import static ru.ncti.backend.config.RabbitConfig.PUBLIC_CHAT_NOTIFICATION;


/**
 * user: ichuvilin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final PublicChatRepository publicChatRepository;
    private final MessageRepository messageRepository;
    private final PrivateChatRepository privateChatRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${minio.url}")
    private String host;

    @Value("${minio.bucket-name}")
    private String bucket;


    public String createPublicChat(ChatRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        PublicChat publicChat = new PublicChat();
        publicChat.getUsers().add(user);

        List<String> list = new ArrayList<>();

        if (!request.getIds().isEmpty()) {
            for (Long id :
                    request.getIds()) {
                User mbUser = userRepository.findById(id).orElse(null);
                publicChat.getUsers().add(mbUser);
                list.add(mbUser.getUsername());
            }
        }

        list.add(user.getUsername());

        publicChat.setName(request.getName());
        publicChatRepository.save(publicChat);

        for (String l : list) {
            simpMessagingTemplate.convertAndSend("/topic/" + l + "/chats",
                    List.of(ViewChatResponse.builder()
                            .id(publicChat.getId())
                            .name(publicChat.getName())
                            .type("PUBLIC")
                            .photo(null)
                            .build()));
        }

        return "Public Chat was created";
    }


    public List<ViewChatResponse> getChatsForUser(Principal principal) {
        String URL = String.format("http://%s:9000/%s/", host, bucket);

        User user = userRepository.findByEmail(principal.getName()).orElseThrow(null);

        List<PublicChat> publicChats = publicChatRepository.findByUsers(user);
        List<PrivateChat> pChat = privateChatRepository.findAllByUser1OrUser2(user, user);

        List<ViewChatResponse> dtos = new ArrayList<>();
        publicChats.forEach(chat -> dtos.add(ViewChatResponse.builder()
                .id(chat.getId())
                .name(chat.getName())
                .type("PUBLIC")
                .photo(chat.getPhoto() == null ? null : (URL + chat.getPhoto()))
                .build()));

        pChat.forEach(chat -> {
            User chatName = chat.getChatName(user);
            dtos.add(ViewChatResponse.builder()
                    .id(chat.getId())
                    .name(String.format("%s %s", chatName.getFirstname(), chatName.getLastname()))
                    .type("PRIVATE")
                    .photo(chatName.getPhoto() == null ? null : (URL + chatName.getPhoto()))
                    .build());
        });

        return dtos;
    }

    @Transactional(readOnly = false)
    public String addUsers(UUID chatId, UsersRequest dto) {
        String URL = String.format("http://%s:9000/%s/", host, bucket);

        PublicChat chat = publicChatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        dto.getIds().forEach(id -> {
                    User user = userRepository.findById(id)
                            .orElse(null);
                    if (user != null) {
                        simpMessagingTemplate.convertAndSend("/topic/" + user.getUsername() + "/chats",
                                List.of(ViewChatResponse.builder()
                                        .id(chat.getId())
                                        .name(chat.getName())
                                        .type("PUBLIC")
                                        .photo(URL + chat.getPhoto())
                                        .build()));
                    }
                    chat.getUsers()
                            .add(user);
                }
        );

        return "Users was added";
    }

    public String leaveChat(UUID chatId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        PublicChat publicChat = publicChatRepository
                .findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Нет такого чата"));

        publicChat.getUsers().removeIf(u -> u.getId().equals(user.getId()));

        if (publicChat.getUsers().isEmpty()) {
            publicChatRepository.delete(publicChat);
            return "Вы вышли из чата";
        }

        publicChatRepository.save(publicChat);
        return "Вы вышли из чата";
    }

    public List<MessageResponse> getMessageFromChat(UUID id, String type) {
        if (type.equals("PUBLIC")) {
            PublicChat chat = publicChatRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
            List<Message> messages = messageRepository.findAllByPublicChatOrderByCreatedAtDesc(chat);

            return generatedMessage(messages);
        } else if (type.equals("PRIVATE")) {
            PrivateChat chat = privateChatRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
            List<Message> messages = messageRepository.findAllByPrivateChatOrderByCreatedAtDesc(chat);

            return generatedMessage(messages);
        }
        return null;
    }

    @Transactional(readOnly = false)
    public MessageResponse sendToPublic(UUID id, MessageRequest dto, Principal principal) {
        PublicChat publicChat = publicChatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Message message = Message.builder()
                .publicChat(publicChat)
                .sender(user)
                .text(dto.getText())
                .type("text")
                .createdAt(Instant.now())
                .build();

        messageRepository.save(message);

        rabbitTemplate.convertAndSend(PUBLIC_CHAT_NOTIFICATION, new HashMap<>() {{
            put("chat", id);
            put("user", user.getUsername());
            put("text", dto.getText());
        }});

        return MessageResponse.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserMessageResponse.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .imageUrl(message.getSender().getPhoto())
                        .build())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build();
    }

    @Transactional(readOnly = false)
    public MessageResponse sendToPrivate(UUID id, String usr, MessageRequest dto, Principal principal) {
        if (usr.equals("user")) {
            PrivateChat privateChat = privateChatRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Message message = Message.builder()
                    .sender(user)
                    .text(dto.getText())
                    .privateChat(privateChat)
                    .type("text")
                    .createdAt(Instant.now())
                    .build();

            messageRepository.save(message);

            return createMessage(message, user);
        } else {
            Optional<PrivateChat> privateChat = privateChatRepository.findById(id);
            if (privateChat.isEmpty()) {
                User first = userRepository.findByEmail(principal.getName())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                User second = userRepository.findByEmail(usr)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Message message = Message.builder()
                        .id(UUID.randomUUID())
                        .sender(first)
                        .text(dto.getText())
                        .type("text")
                        .createdAt(Instant.now())
                        .build();

                PrivateChat newPrivateChat = PrivateChat.builder()
                        .id(id)
                        .user1(first)
                        .user2(second)
                        .messages(List.of(message))
                        .build();
                message.setPrivateChat(newPrivateChat);
                privateChatRepository.save(newPrivateChat);

                CompletableFuture.runAsync(() -> {
                    log.info("Async task started");
                    log.info(privateChat.get().getId().toString());
                    // Ожидание успешного сохранения чата и сообщения
                    PrivateChat chat = privateChatRepository.findById(id)
                            .orElse(null); // Передайте null, если чат не найден

                    if (chat != null) {
                        String username = first.getUsername();
                        String text = message.getText();

                        HashMap<String, Object> messageData = new HashMap<>();
                        messageData.put("chat", chat.getId());
                        messageData.put("user", username);
                        messageData.put("text", text);

                        rabbitTemplate.convertAndSend(PRIVATE_CHAT_NOTIFICATION, messageData);
                    }

                    log.info("Async task completed");
                });

                return MessageResponse.builder()
                        .id(message.getId())
                        .text(message.getText())
                        .type("text")
                        .author(UserMessageResponse.builder()
                                .id(String.valueOf(message.getSender().getId()))
                                .firstName(message.getSender().getFirstname())
                                .lastName(message.getSender().getLastname())
                                .imageUrl(message.getSender().getPhoto())
                                .build())
                        .createdAt(message.getCreatedAt().toEpochMilli())
                        .build();
            } else {
                User user = userRepository.findByEmail(principal.getName())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Message message = Message.builder()
                        .sender(user)
                        .type("text")
                        .text(dto.getText())
                        .privateChat(privateChat.get())
                        .createdAt(Instant.now())
                        .build();

                messageRepository.save(message);
                return createMessage(message, user);
            }
        }
    }

    private List<MessageResponse> generatedMessage(List<Message> messages) {
        String URL = String.format("http://%s:9000/%s/", host, bucket);

        return messages.stream().map(message -> {
            String photo = message.getSender().getPhoto() == null ? null : URL + message.getSender().getPhoto();
            if (message.getType().equals("image")) {
                return MessageResponse.builder()
                        .id(message.getId())
                        .uri(URL + message.getText())
                        .name(message.getText())
                        .type(message.getType())
                        .size(message.getText().length())
                        .author(UserMessageResponse.builder()
                                .id(String.valueOf(message.getSender().getId()))
                                .firstName(message.getSender().getFirstname())
                                .lastName(message.getSender().getLastname())
                                .imageUrl(photo)
                                .build())
                        .createdAt(message.getCreatedAt().toEpochMilli())
                        .build();
            } else {
                return MessageResponse.builder()
                        .id(message.getId())
                        .text(message.getText())
                        .type(message.getType())
                        .author(UserMessageResponse.builder()
                                .id(String.valueOf(message.getSender().getId()))
                                .firstName(message.getSender().getFirstname())
                                .lastName(message.getSender().getLastname())
                                .imageUrl(photo)
                                .build())
                        .createdAt(message.getCreatedAt().toEpochMilli())
                        .build();
            }
        }).toList();
    }

    private MessageResponse createMessage(Message message, User user) {
        rabbitTemplate.convertAndSend(PRIVATE_CHAT_NOTIFICATION, new HashMap<>() {{
            put("chat", message.getPrivateChat().getId());
            put("user", user.getUsername());
            put("text", message.getText());
        }});

        return MessageResponse.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserMessageResponse.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .imageUrl(message.getSender().getPhoto())
                        .build())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build();
    }
}
