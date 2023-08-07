package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ncti.backend.api.request.ChatRequest;
import ru.ncti.backend.api.request.MessageRequest;
import ru.ncti.backend.api.request.UsersRequest;
import ru.ncti.backend.api.response.MessageResponse;
import ru.ncti.backend.api.response.ViewChatResponse;
import ru.ncti.backend.service.ChatService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * user: ichuvilin
 */
@Controller
@RequestMapping("/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatService chatService;

    @SubscribeMapping("/topic/{email}/chats")
    public void getChatsForUser(Principal principal) {
        List<ViewChatResponse> views = chatService.getChatsForUser(principal);
        simpMessagingTemplate.convertAndSend("/topic/" + principal.getName() + "/chats", views);
    }

    @PostMapping("/{chatId}/logout")
    public ResponseEntity<String> leaveChat(@PathVariable("chatId") UUID uuid) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.leaveChat(uuid));
    }

    @PostMapping("/{chatId}")
    public ResponseEntity<String> addUsers(@PathVariable("chatId") UUID id, @RequestBody UsersRequest dto) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.addUsers(id, dto));
    }

    @PostMapping("/create-public")
    public ResponseEntity<String> createPublicChat(@RequestBody ChatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createPublicChat(request));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<List<MessageResponse>> getChatMessages(@PathVariable("chatId") UUID id, @Payload("type") String type) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.getMessageFromChat(id, type));
    }

    @MessageMapping("/{chatId}/chat")
    public void handlePublicMessage(@DestinationVariable("chatId") UUID id, MessageRequest message, Principal principal) {
        MessageResponse mes = chatService.sendToPublic(id, message, principal);
        simpMessagingTemplate.convertAndSend("/topic/public/" + id, mes);
    }

    @MessageMapping("/{chatId}/{user}")
    public void handlePrivateMessage(@DestinationVariable("chatId") UUID id, @DestinationVariable("user") String user, MessageRequest message, Principal principal) {
        MessageResponse mes = chatService.sendToPrivate(id, user, message, principal);
        simpMessagingTemplate.convertAndSend("/topic/private/" + id, mes);
    }
}
