package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.entity.dto.ChatRequest;
import com.anti.entity.vo.ChatVO;
import com.anti.security.LoginUser;
import com.anti.service.QAConversationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerAuthTest {

    @Test
    void askRequiresAuthenticatedUser() {
        ChatController controller = new ChatController(mock(QAConversationService.class));
        ChatRequest request = new ChatRequest();
        request.setQuestion("如何识别刷单诈骗");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.ask(request, null));

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getMessage()).contains("请先登录");
    }

    @Test
    void askUsesAuthenticationPrincipalUserId() {
        QAConversationService service = mock(QAConversationService.class);
        ChatController controller = new ChatController(service);
        ChatRequest request = new ChatRequest();
        request.setQuestion("如何识别刷单诈骗");
        request.setSessionId("session_7_1000");
        ChatVO chat = new ChatVO();
        when(service.askQuestion("如何识别刷单诈骗", "session_7_1000", 7L, "auto")).thenReturn(chat);

        assertThat(controller.ask(request, new LoginUser(7L, "student", "student")).getData())
                .isSameAs(chat);
        verify(service).askQuestion("如何识别刷单诈骗", "session_7_1000", 7L, "auto");
    }
}
