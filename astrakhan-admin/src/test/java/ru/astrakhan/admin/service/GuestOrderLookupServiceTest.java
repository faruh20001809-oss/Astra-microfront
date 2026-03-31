package ru.astrakhan.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.repository.OrderRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestOrderLookupServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEmailService orderEmailService;

    @InjectMocks
    private GuestOrderLookupService service;

    @Test
    void requestCodeShouldNotSendMailWhenNoOrdersFound() {
        when(orderRepository.findByEmailIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("user@mail.com"))
                .thenReturn(List.of());

        service.requestCode(" user@mail.com ");

        verify(orderEmailService, never()).sendGuestOrderLookupCodeAsync(anyString(), anyString());
    }

    @Test
    void requestCodeShouldRateLimitSecondRequest() {
        when(orderRepository.findByEmailIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("user@mail.com"))
                .thenReturn(List.of(Order.builder().orderId("ORD-1").build()));

        service.requestCode("user@mail.com");

        assertThrows(
                GuestOrderLookupService.LookupRateLimitedException.class,
                () -> service.requestCode("user@mail.com")
        );
    }

    @Test
    void verifyAndListOrdersShouldReturnListWhenCodeIsCorrect() {
        Order order = Order.builder().orderId("ORD-OK").build();
        when(orderRepository.findByEmailIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("user@mail.com"))
                .thenReturn(List.of(order));

        service.requestCode("user@mail.com");
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderEmailService).sendGuestOrderLookupCodeAsync(org.mockito.ArgumentMatchers.eq("user@mail.com"), codeCaptor.capture());

        List<Order> result = service.verifyAndListOrders("user@mail.com", codeCaptor.getValue());
        assertEquals(1, result.size());
        assertEquals("ORD-OK", result.get(0).getOrderId());
    }

    @Test
    void verifyAndListOrdersShouldRejectWrongCode() {
        when(orderRepository.findByEmailIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc("user@mail.com"))
                .thenReturn(List.of(Order.builder().orderId("ORD-1").build()));

        service.requestCode("user@mail.com");

        assertThrows(
                GuestOrderLookupService.LookupAuthException.class,
                () -> service.verifyAndListOrders("user@mail.com", "000000")
        );
    }
}
