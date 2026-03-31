package ru.astrakhan.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.repository.RewardRedemptionEventRepository;
import ru.astrakhan.admin.repository.RouteCompletionEventRepository;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteProgressServiceTest {

    @Mock
    private RouteService routeService;
    @Mock
    private RouteCompletionEventRepository completionRepository;
    @Mock
    private RewardRedemptionEventRepository redemptionRepository;

    @InjectMocks
    private RouteProgressService service;

    @Test
    void markCompletedShouldPersistOnlyForNewRouteAndReturnStats() {
        Route paidRoute = Route.builder().id(10L).paid(true).build();
        when(routeService.findById(10L)).thenReturn(Optional.of(paidRoute));
        when(completionRepository.existsByEmailIgnoreCaseAndRouteId("user@mail.com", 10L)).thenReturn(false);
        when(completionRepository.countByEmailIgnoreCaseAndPaidTrue("user@mail.com")).thenReturn(1L);
        when(completionRepository.countByEmailIgnoreCaseAndPaidFalse("user@mail.com")).thenReturn(0L);
        when(redemptionRepository.countByEmailIgnoreCase("user@mail.com")).thenReturn(0L);

        Map<String, Object> result = service.markCompleted(" User@Mail.com ", 10L);

        verify(completionRepository).save(any());
        assertEquals("user@mail.com", result.get("email"));
        assertEquals(1L, ((Number) result.get("completedPaidRoutes")).longValue());
        assertEquals(1L, ((Number) result.get("completedRoutes")).longValue());
    }

    @Test
    void redeemRewardShouldRejectFreeRoute() {
        Route freeRoute = Route.builder().id(15L).paid(false).build();
        when(routeService.findById(15L)).thenReturn(Optional.of(freeRoute));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.redeemReward("user@mail.com", 15L)
        );

        assertTrue(ex.getMessage().contains("платному маршруту"));
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void redeemRewardShouldSaveWhenRewardIsAvailable() {
        Route paidRoute = Route.builder().id(20L).paid(true).build();
        when(routeService.findById(20L)).thenReturn(Optional.of(paidRoute));
        when(completionRepository.countByEmailIgnoreCaseAndPaidTrue("user@mail.com")).thenReturn(2L);
        when(completionRepository.countByEmailIgnoreCaseAndPaidFalse("user@mail.com")).thenReturn(3L);
        when(redemptionRepository.countByEmailIgnoreCase("user@mail.com")).thenReturn(0L);

        Map<String, Object> result = service.redeemReward("user@mail.com", 20L);

        verify(redemptionRepository).save(any());
        assertEquals(1L, ((Number) result.get("availableRewards")).longValue());
    }
}
