package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.dto.AddUserDTO;
import ru.ncti.backend.dto.ChatDTO;
import ru.ncti.backend.dto.ChatViewDTO;
import ru.ncti.backend.dto.MessageDTO;
import ru.ncti.backend.dto.MessageFromChatDTO;
import ru.ncti.backend.dto.PrivateChatDTO;
import ru.ncti.backend.dto.UserFromMessageDTO;
import ru.ncti.backend.entity.Chat;
import ru.ncti.backend.entity.ChatMessage;
import ru.ncti.backend.entity.Message;
import ru.ncti.backend.entity.PrivateChat;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.ChatMessageRepository;
import ru.ncti.backend.repository.ChatRepository;
import ru.ncti.backend.repository.MessageRepository;
import ru.ncti.backend.repository.PrivateChatRepository;
import ru.ncti.backend.repository.UserRepository;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ru.ncti.backend.dto.RabbitQueue.PRIVATE_CHAT_NOTIFICATION;
import static ru.ncti.backend.dto.RabbitQueue.PUBLIC_CHAT_NOTIFICATION;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */

@Log4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final PrivateChatRepository privateChatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RabbitTemplate rabbitTemplate;

    public String createPublicChat(ChatDTO dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        Chat chat = new Chat();
        chat.getUsers().add(user);

        if (!dto.getIds().isEmpty()) {
            for (Long id :
                    dto.getIds()) {
                chat.getUsers().add(userRepository.getById(id));
            }
        }

        chat.setName(dto.getName());
        chatRepository.save(chat);
        return "Public Chat was created";
    }

    @Transactional(readOnly = false)
    public String createPrivateChat(PrivateChatDTO dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        User user2 = userRepository.findById(dto.getUser())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PrivateChat privateChat = new PrivateChat();
        privateChat.setUser1(user);
        privateChat.setUser2(user2);
        privateChatRepository.save(privateChat);

        return "Private Chat was created";
    }

    public List<ChatViewDTO> getChatsFromUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        List<Chat> chats = chatRepository.findByUser(user);
        List<PrivateChat> pChat = privateChatRepository.findAllByUser1OrUser2(user, user);
        log.info(pChat);
        List<ChatViewDTO> dtos = new ArrayList<>();
        chats.forEach(chat -> dtos.add(ChatViewDTO.builder()
                .id(chat.getId())
                .name(chat.getName())
                .type("PUBLIC")
                .build()));

        pChat.forEach(chat -> {
            User chatname = chat.getChatName(user);
            dtos.add(ChatViewDTO.builder()
                    .id(chat.getId())
                    .name(String.format("%s %s", chatname.getFirstname(), chatname.getLastname()))
                    .type("PRIVATE")
                    .build());
        });

        return dtos;
    }

    @Transactional(readOnly = false)
    public String addUsersToChats(UUID chatId, AddUserDTO dto) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        dto.getIds().forEach(id -> chat.getUsers()
                .add(userRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"))));

        return "Users was added";
    }

    public List<MessageFromChatDTO> getMessageFromChat(UUID id, String type) {
        if (type.equals("PUBLIC")) {
            Chat chat = chatRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
            List<ChatMessage> messages = chatMessageRepository.findAllByChatOrderByCreatedAtDesc(chat);
            List<MessageFromChatDTO> dtos = new ArrayList<>(messages.size());
            messages.forEach(message -> dtos.add(MessageFromChatDTO.builder()
                    .id(message.getId())
                    .text(message.getText())
                    .type("text")
                    .author(UserFromMessageDTO.builder()
                            .id(String.valueOf(message.getSender().getId()))
                            .firstName(message.getSender().getFirstname())
                            .lastName(message.getSender().getLastname())
                            .build())
                    .createdAt(message.getCreatedAt().toEpochMilli())
                    .build()));

            return dtos;
        } else if (type.equals("PRIVATE")) {
            PrivateChat chat = privateChatRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
            List<Message> messages = messageRepository.findAllByPrivateChatOrderByCreatedAtDesc(chat);
            List<MessageFromChatDTO> dtos = new ArrayList<>(messages.size());

            messages.forEach(message -> dtos.add(MessageFromChatDTO.builder()
                    .id(message.getId())
                    .text(message.getText())
                    .type("text")
                    .author(UserFromMessageDTO.builder()
                            .id(String.valueOf(message.getSender().getId()))
                            .firstName(message.getSender().getFirstname())
                            .lastName(message.getSender().getLastname())
                            .build())
                    .createdAt(message.getCreatedAt().toEpochMilli())
                    .build()));

            return dtos;
        } else {
            throw new IllegalArgumentException("Invalid chat type");
        }
    }


    @Transactional(readOnly = false)
    public MessageFromChatDTO sendToPublic(UUID id, MessageDTO dto, Principal principal) {
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .sender(user)
                .text(dto.getText())
                .createdAt(Instant.now())
                .build();

        chatMessageRepository.save(message);

        rabbitTemplate.convertAndSend(PUBLIC_CHAT_NOTIFICATION, new HashMap<>() {{
            put("chat", id);
            put("user", user.getUsername());
            put("text", dto.getText());
        }});

        return MessageFromChatDTO.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserFromMessageDTO.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .build())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build();
    }

    @Transactional(readOnly = false)
    public MessageFromChatDTO sendToPrivate(UUID id, MessageDTO dto, Principal principal) {
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

        rabbitTemplate.convertAndSend(PRIVATE_CHAT_NOTIFICATION, new HashMap<>() {{
            put("chat", id);
            put("user", user.getUsername());
            put("text", dto.getText());
        }});

        return MessageFromChatDTO.builder()
                .id(message.getId())
                .text(message.getText())
                .type("text")
                .author(UserFromMessageDTO.builder()
                        .id(String.valueOf(message.getSender().getId()))
                        .firstName(message.getSender().getFirstname())
                        .lastName(message.getSender().getLastname())
                        .build())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build();
    }
}
