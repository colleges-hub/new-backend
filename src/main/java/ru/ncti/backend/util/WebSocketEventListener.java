package ru.ncti.backend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import ru.ncti.backend.model.ChatData;
import ru.ncti.backend.model.UserInChat;
import ru.ncti.backend.repository.ChatDataRepository;
import ru.ncti.backend.repository.UserInChatRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * user: ichuvilin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final ChatDataRepository chatDataRepository;
    private final UserInChatRepository userInChatRepository;

    @EventListener
    public void handlerSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String uuid = Objects.requireNonNull(headers.getHeader("simpDestination")).toString().split("/")[3].trim();
        if (!uuid.equals("chats")) {
            String name = Objects.requireNonNull(headers.getUser()).getName();
            ChatData chatData = ChatData.builder()
                    .id(headers.getSessionId())
                    .username(name)
                    .chat(uuid)
                    .build();

            UserInChat chat = userInChatRepository.findById(uuid).orElse(null);
            if (chat != null) {
                chat.getEmail().add(name);
            } else {
                chat = UserInChat.builder()
                        .id(uuid)
                        .email(Set.of(name))
                        .build();
            }
            userInChatRepository.save(chat);
            chatDataRepository.save(chatData);
        }
    }

    @EventListener
    public void handlerUnsubscribe(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        chatDataRepository.findByUsername(Objects.requireNonNull(headers.getUser()).getName()).ifPresent(chatData -> log.info(chatData.getUsername()));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Optional<ChatData> chatData = chatDataRepository.findById(Objects.requireNonNull(headers.getSessionId()));
        if (chatData.isPresent()) {
            Optional<UserInChat> byId = userInChatRepository.findById(chatData.get().getChat());
            byId.ifPresent(chat -> chat.getEmail().remove(chatData.get().getUsername()));
            if (byId.isPresent() && byId.get().getEmail().size() == 0) {
                userInChatRepository.delete(byId.get());
            } else
                userInChatRepository.save(byId.get());
            chatDataRepository.delete(chatData.get());
        }
    }

}
