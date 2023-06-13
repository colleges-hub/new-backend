package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import static ru.ncti.backend.dto.RabbitQueue.FIRST_MESSAGE;
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

    public String leaveChat(UUID chatId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        Chat chat = chatRepository
                .findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Нет такого чата"));

        chat.getUsers().removeIf(u -> u.getId().equals(user.getId()));

        if (chat.getUsers().isEmpty()) {
            chatRepository.delete(chat);
            return "Вы вышли из чата";
        }

        chatRepository.save(chat);
        return "Вы вышли из чата";
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
    public MessageFromChatDTO sendToPrivate(UUID id, String usr, MessageDTO dto, Principal principal) {
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
            if (privateChatRepository.findById(id).isEmpty()) {
                User first = userRepository.findByEmail(principal.getName())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                User second = userRepository.findByEmail(usr)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Message message = Message.builder()
                        .sender(first)
                        .text(dto.getText())
                        .createdAt(Instant.now())
                        .build();

                PrivateChat privateChat = PrivateChat.builder()
                        .id(id)
                        .user1(first)
                        .user2(second)
                        .messages(List.of(message))
                        .build();

                message.setPrivateChat(privateChat);
                privateChatRepository.save(privateChat);
                rabbitTemplate.convertAndSend(FIRST_MESSAGE, new HashMap<>() {{
                    put("user", second.getId().toString());
                    put("name", String.format("%s %s", first.getFirstname(), first.getLastname()));
                    put("chat", privateChat.getId().toString());
                    put("text", message.getText());
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
            } else {
                //todo
                return null;
            }
        }
    }

    private MessageFromChatDTO createMessage(Message message, User user) {
        rabbitTemplate.convertAndSend(PRIVATE_CHAT_NOTIFICATION, new HashMap<>() {{
            put("chat", message.getPrivateChat().getId());
            put("user", user.getUsername());
            put("text", message.getText());
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

    @RabbitListener(queues = FIRST_MESSAGE)
    private void updateMessage(List<?> list) {
        PrivateChat privateChat = (PrivateChat) list.get(0);
        Message message = (Message) list.get(1);
        message.setPrivateChat(privateChat);
        messageRepository.save(message);
    }

}
