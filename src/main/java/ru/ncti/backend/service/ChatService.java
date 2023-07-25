package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import static ru.ncti.backend.config.RabbitConfig.PRIVATE_CHAT_NOTIFICATION;


/**
 * user: ichuvilin
 */
@Log4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final PublicChatRepository publicChatRepository;
    private final MessageRepository messageRepository;
    private final PrivateChatRepository privateChatRepository;
    private final RabbitTemplate rabbitTemplate;

    public String createPublicChat(ChatRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        PublicChat publicChat = new PublicChat();
        publicChat.getUsers().add(user);

        if (!request.getIds().isEmpty()) {
            for (Long id :
                    request.getIds()) {
                publicChat.getUsers().add(userRepository.findById(id).orElse(null));
            }
        }

        publicChat.setName(request.getName());
        publicChatRepository.save(publicChat);
        return "Public Chat was created";
    }

    public List<ViewChatResponse> getChatsFromUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        List<PublicChat> publicChats = publicChatRepository.findByUsers(user);
        List<PrivateChat> pChat = privateChatRepository.findAllByUser1OrUser2(user, user);
        log.info(pChat);
        List<ViewChatResponse> dtos = new ArrayList<>();
        publicChats.forEach(chat -> dtos.add(ViewChatResponse.builder()
                .id(chat.getId())
                .name(chat.getName())
                .type("PUBLIC")
                .photo(chat.getPhoto())
                .build()));

        pChat.forEach(chat -> {
            User chatName = chat.getChatName(user);
            dtos.add(ViewChatResponse.builder()
                    .id(chat.getId())
                    .name(String.format("%s %s", chatName.getFirstname(), chatName.getLastname()))
                    .type("PRIVATE")
                    .photo(chatName.getPhoto())
                    .build());
        });

        return dtos;
    }

    @Transactional(readOnly = false)
    public String addUsersToChats(UUID chatId, UsersRequest dto) {
        PublicChat publicChat = publicChatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        dto.getIds().forEach(id -> publicChat.getUsers()
                .add(userRepository.findById(id)
                        .orElse(null)));

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
            PublicChat publicChat = publicChatRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
            List<Message> messages = messageRepository.findAllByPublicChatOrderByCreatedAtDesc(publicChat);

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
                .createdAt(Instant.now())
                .build();

        messageRepository.save(message);

//        rabbitTemplate.convertAndSend(PUBLIC_CHAT_NOTIFICATION, new HashMap<>() {{
//            put("chat", id);
//            put("user", user.getUsername());
//            put("text", dto.getText());
//        }});

        return MessageResponse.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserMessageResponse.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .photo(message.getSender().getPhoto())
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
                        .sender(first)
                        .text(dto.getText())
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
                return createMessage(message, first);
            } else {
                User user = userRepository.findByEmail(principal.getName())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Message message = Message.builder()
                        .sender(user)
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
        return messages.stream().map(message -> MessageResponse.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserMessageResponse.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .photo(message.getSender().getPhoto())
                        .build())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build()).toList();
    }

    private MessageResponse createMessage(Message message, User user) {
        rabbitTemplate.convertAndSend(PRIVATE_CHAT_NOTIFICATION, new HashMap<>() {{
            put("chat", message.getPrivateChat().getId());
            put("user", user.getUsername());
            put("text", message.getText());
        }});
        log.info(message.getId());
        return MessageResponse.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserMessageResponse.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .photo(message.getSender().getPhoto())
                        .build())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build();
    }
}
