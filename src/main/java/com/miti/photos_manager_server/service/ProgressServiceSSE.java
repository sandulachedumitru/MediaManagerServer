package com.miti.photos_manager_server.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

public interface ProgressServiceSSE {
    SseEmitter addEEmitter();
    void sendProgressUpdate(final long progress);
}
