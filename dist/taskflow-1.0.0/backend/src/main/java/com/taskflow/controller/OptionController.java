package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.property.OptionDetailResponse;
import com.taskflow.dto.property.OptionUpdateRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 옵션 컨트롤러
 *
 * API:
 * - GET /api/options/{id} - 옵션 조회
 * - PUT /api/options/{id} - 옵션 수정
 * - DELETE /api/options/{id} - 옵션 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/options")
@RequiredArgsConstructor
public class OptionController {

    private final PropertyService propertyService;

    /**
     * 옵션 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OptionDetailResponse>> getOption(
            @PathVariable("id") Long optionId
    ) {
        log.debug("Get option: id={}", optionId);

        OptionDetailResponse response = propertyService.getOption(optionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 옵션 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OptionDetailResponse>> updateOption(
            @PathVariable("id") Long optionId,
            @Valid @RequestBody OptionUpdateRequest request
    ) {
        log.info("Update option: id={}", optionId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        OptionDetailResponse response = propertyService.updateOption(optionId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "옵션이 수정되었습니다"));
    }

    /**
     * 옵션 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOption(
            @PathVariable("id") Long optionId
    ) {
        log.info("Delete option: id={}", optionId);

        propertyService.deleteOption(optionId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("옵션이 삭제되었습니다"));
    }
}
