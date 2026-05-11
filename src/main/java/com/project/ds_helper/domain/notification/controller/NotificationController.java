package com.project.ds_helper.domain.notification.controller;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.notification.dto.response.GetNotificationsResponseDto;
import com.project.ds_helper.domain.notification.dto.response.UnreadNotificationCountResponseDto;
import com.project.ds_helper.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Tag(name = SwaggerTagName.NOTIFICATION)
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림을 최신순으로 커서 기반 조회합니다.")
    @GetMapping("/notifications")
    public ResponseEntity<ResponseVo<CursorResponseDto<GetNotificationsResponseDto>>> getMyNotifications(
            Authentication authentication,
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            @RequestParam(name = "cursor-time", required = false) LocalDateTime cursorTime,
            @Parameter(description = "커서용 ID. Notification Entity의 notificationId와 동일한 UUID 문자열 형식입니다.", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
            @RequestParam(name = "cursor-id", required = false) String cursorId,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        CursorResponseDto<GetNotificationsResponseDto> responseDto =
                notificationService.getMyNotifications(authentication, cursorTime, cursorId, size);
        ResponseVo<CursorResponseDto<GetNotificationsResponseDto>> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ResponseVo<Void>> markAsRead(
            Authentication authentication,
            @PathVariable String notificationId
    ) {
        notificationService.markAsRead(authentication, notificationId);
        ResponseVo<Void> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), null);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "로그인한 사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ResponseVo<Void>> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication);
        ResponseVo<Void> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), null);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    @Operation(summary = "미읽음 알림 개수 조회", description = "로그인한 사용자의 미읽음 알림 개수를 조회합니다.")
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ResponseVo<UnreadNotificationCountResponseDto>> getUnreadNotificationCount(Authentication authentication) {
        UnreadNotificationCountResponseDto responseDto =
                notificationService.getUnreadNotificationCount(authentication);
        ResponseVo<UnreadNotificationCountResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), responseDto);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
