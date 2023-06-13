package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.dto.AddUserDTO;
import ru.ncti.backend.dto.ChatDTO;
import ru.ncti.backend.dto.ChatViewDTO;
import ru.ncti.backend.dto.MessageDTO;
import ru.ncti.backend.dto.MessageFromChatDTO;
import ru.ncti.backend.service.ChatService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Log4j
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatService chatService;


    @GetMapping()
    public ResponseEntity<List<ChatViewDTO>> getChatsFromUser() {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.getChatsFromUser());
    }

    @PostMapping("/{chatId}/logout")
    public ResponseEntity<String> leaveChat(@PathVariable("chatId") UUID uuid) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.leaveChat(uuid));
    }

    @PostMapping("/{chatId}")
    public ResponseEntity<String> addUsersToChat(@PathVariable("chatId") UUID id, @RequestBody AddUserDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.addUsersToChats(id, dto));
    }

    @PostMapping("/create-public")
    public ResponseEntity<String> createPublicChat(@RequestBody ChatDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createPublicChat(dto));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<List<MessageFromChatDTO>> getChatMessages(@PathVariable("chatId") UUID id, @Payload("type") String type) {
        log.info(type);
        return ResponseEntity.status(HttpStatus.OK).body(chatService.getMessageFromChat(id, type));
    }

    @MessageMapping("/{chatId}/chat")
    public void handlePublicMessage(@DestinationVariable("chatId") UUID id, MessageDTO message, Principal principal) {
        MessageFromChatDTO mes = chatService.sendToPublic(id, message, principal);
        simpMessagingTemplate.convertAndSend("/topic/public/" + id, mes);
    }

    @MessageMapping("/{chatId}/{user}")
    public void handlePrivateMessage(@DestinationVariable("chatId") UUID id, @DestinationVariable("user") String user, MessageDTO message, Principal principal) {
        MessageFromChatDTO mes = chatService.sendToPrivate(id, user, message, principal);
        simpMessagingTemplate.convertAndSend("/topic/private/" + id, mes);
    }
}
