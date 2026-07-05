package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.entity.vo.AgentChallengeSessionVO;
import com.anti.security.LoginUser;
import com.anti.service.AgentChallengeService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentChallengeControllerAuthTest {

    @Test
    void startRequiresAuthenticatedUser() {
        AgentChallengeController controller = new AgentChallengeController(mock(AgentChallengeService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.startChallenge(99L, null));

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getMessage()).contains("请先登录");
    }

    @Test
    void startUsesAuthenticationPrincipalUserId() {
        AgentChallengeService service = mock(AgentChallengeService.class);
        AgentChallengeSessionVO vo = new AgentChallengeSessionVO();
        when(service.startChallenge(99L, 1L)).thenReturn(vo);
        AgentChallengeController controller = new AgentChallengeController(service);

        assertThat(controller.startChallenge(99L, new LoginUser(1L, "student", "student")).getData())
                .isSameAs(vo);
        verify(service).startChallenge(99L, 1L);
    }
}
