package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.security.SecurityUtils;
import com.taskflow.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 컨트롤러
 *
 * API:
 * - GET /api/sse/subscribe - SSE 연결 수립
 * - POST /api/sse/boards/{boardId}/subscribe - 보드 구독
 * - DELETE /api/sse/boards/{boardId}/subscribe - 보드 구독 해제
 * - GET /api/sse/status - 연결 상태 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager emitterManager;

    /**
     * SSE 연결 수립
     *
     * 클라이언트와 SSE 연결을 수립합니다.
     * 연결 후 3초마다 하트비트가 전송되어 연결이 유지됩니다.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("SSE subscribe request from user: {}", currentUserId);

        return emitterManager.createEmitter(currentUserId);
    }

    /**
     * 보드 구독
     *
     * 특정 보드의 이벤트를 구독합니다.
     * 보드에서 발생하는 item:created, item:updated 등의 이벤트를 수신합니다.
     */
    @PostMapping("/boards/{boardId}/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribeBoard(
            @PathVariable("boardId") Long boardId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Board subscribe request: user={}, boardId={}", currentUserId, boardId);

        emitterManager.subscribeBoard(currentUserId, boardId);

        return ResponseEntity.ok(ApiResponse.success(null, "보드 구독이 완료되었습니다"));
    }

    /**
     * 보드 구독 해제
     *
     * 특정 보드의 이벤트 구독을 해제합니다.
     */
    @DeleteMapping("/boards/{boardId}/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribeBoard(
            @PathVariable("boardId") Long boardId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Board unsubscribe request: user={}, boardId={}", currentUserId, boardId);

        emitterManager.unsubscribeBoard(currentUserId, boardId);

        return ResponseEntity.ok(ApiResponse.success(null, "보드 구독이 해제되었습니다"));
    }

    /**
     * SSE 연결 상태 조회
     *
     * 현재 SSE 연결 상태와 총 연결 수를 조회합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SseStatusResponse>> getStatus() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        boolean connected = emitterManager.isConnected(currentUserId);
        int totalConnections = emitterManager.getConnectionCount();

        SseStatusResponse response = new SseStatusResponse(connected, totalConnections);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * SSE 연결 해제
     *
     * 현재 사용자의 SSE 연결을 수동으로 해제합니다.
     */
    @DeleteMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("SSE unsubscribe request from user: {}", currentUserId);

        emitterManager.removeEmitter(currentUserId);

        return ResponseEntity.ok(ApiResponse.success(null, "SSE 연결이 해제되었습니다"));
    }

    /**
     * SSE 상태 응답 DTO
     */
    public record SseStatusResponse(boolean connected, int totalConnections) {
    }
}
