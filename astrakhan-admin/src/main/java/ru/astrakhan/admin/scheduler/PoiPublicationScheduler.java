package ru.astrakhan.admin.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.astrakhan.admin.service.PoiService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoiPublicationScheduler {

    private final PoiService poiService;

    /** Каждые 2 минуты: публикация по publishAt и архив по unpublishAt. */
    @Scheduled(cron = "0 */2 * * * *")
    public void run() {
        try {
            poiService.applyScheduledPublicationWindow();
        } catch (Exception e) {
            log.warn("Scheduled POI publication failed: {}", e.getMessage());
        }
    }
}
