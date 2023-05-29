package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.dto.AddUserDTO;
import ru.ncti.backend.dto.ChatDTO;
import ru.ncti.backend.dto.ChatViewDTO;
import ru.ncti.backend.dto.MessageDTO;
import ru.ncti.backend.dto.MessageFromChatDTO;
import ru.ncti.backend.dto.UserFromMessageDTO;
import ru.ncti.backend.entity.Chat;
import ru.ncti.backend.entity.Message;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.ChatRepository;
import ru.ncti.backend.repository.MessageRepository;
import ru.ncti.backend.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */

@Log4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    public String createChat(ChatDTO dto) {
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
        return "OK";
    }

    public List<ChatViewDTO> getChatsFromUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        List<Chat> chats = chatRepository.findByUser(user);
        List<ChatViewDTO> dtos = new ArrayList<>();
        chats.forEach(chat -> dtos.add(ChatViewDTO.builder()
                .id(chat.getId())
                .name(chat.getName())
                .userCount(chat.getUsers().size())
                .build()));

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

    @Transactional(readOnly = false)
    public MessageFromChatDTO sendMessage(UUID chatId, MessageDTO dto) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        User user = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Message message = Message.builder()
                .chat(chat)
                .sender(user)
                .text(dto.getText())
                .createdAt(Instant.now())
                .build();

        messageRepository.save(message);

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

    public List<MessageFromChatDTO> getMessageFromChat(UUID chatId) {
        log.info("OK");
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        List<Message> messages = messageRepository.findAllByChatOrderByCreatedAtDesc(chat);

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
    }


}
