package com.example.evaluation.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationService(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void broadcastNewTask(Long teacherId, String taskTitle) {
        Map<String, Object> data = Map.of(
                "teacherId", teacherId,
                "title", taskTitle,
                "message", "教师发布了新实训任务：" + taskTitle);
        webSocketHandler.sendToAll("NEW_TASK", data);
    }
}
