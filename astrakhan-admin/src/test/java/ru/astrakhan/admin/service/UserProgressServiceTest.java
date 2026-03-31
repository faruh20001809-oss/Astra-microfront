package ru.astrakhan.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.astrakhan.admin.entity.UserProgressProfile;
import ru.astrakhan.admin.repository.UserProgressProfileRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProgressServiceTest {

    private final UserProgressProfileRepository repository = mock(UserProgressProfileRepository.class);
    private final UserProgressService service = new UserProgressService(repository, new ObjectMapper());

    @Test
    void getSnapshotByEmailShouldReturnEmptySnapshotForUnknownUser() {
        when(repository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.empty());

        Map<String, Object> result = service.getSnapshotByEmail(" User@mail.com ");

        assertEquals("user@mail.com", result.get("email"));
        assertNotNull(result.get("snapshot"));
    }

    @Test
    void getSnapshotByEmailShouldGracefullyHandleBrokenJson() {
        UserProgressProfile profile = UserProgressProfile.builder()
                .email("user@mail.com")
                .snapshotJson("{broken_json")
                .updatedAt(LocalDateTime.now())
                .build();
        when(repository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(profile));

        Map<String, Object> result = service.getSnapshotByEmail("user@mail.com");

        assertEquals("user@mail.com", result.get("email"));
        assertEquals(Map.of(), result.get("snapshot"));
    }

    @Test
    void upsertSnapshotShouldNormalizeEmailAndPersistJson() {
        when(repository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.empty());
        when(repository.save(any(UserProgressProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.upsertSnapshot(" User@Mail.com ", Map.of("progress", Map.of("free", 3)));

        verify(repository).save(any(UserProgressProfile.class));
        assertEquals("user@mail.com", result.get("email"));
        Map<?, ?> snapshot = (Map<?, ?>) result.get("snapshot");
        assertEquals(3, ((Number) ((Map<?, ?>) snapshot.get("progress")).get("free")).intValue());
    }

    @Test
    void upsertSnapshotShouldRejectInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> service.upsertSnapshot("invalid-email", Map.of()));
    }
}
