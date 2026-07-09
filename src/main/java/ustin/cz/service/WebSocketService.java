package ustin.cz.service;

import ustin.cz.component.Message;


public interface WebSocketService {

    void sendMessage(String sessionId, Message message);

}
