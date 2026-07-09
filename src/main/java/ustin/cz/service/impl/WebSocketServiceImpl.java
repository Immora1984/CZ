package ustin.cz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ustin.cz.component.Message;
import ustin.cz.service.WebSocketService;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendMessage(String sessionId, Message message) {
        try {
            messagingTemplate.convertAndSend("/topic/progress-" + sessionId, message);
            log.info("📤 Сообщение отправлено в топик /topic/progress-{}: {}", sessionId, message.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения в топик /topic/progress-{}", sessionId, e);
        }
    }
}