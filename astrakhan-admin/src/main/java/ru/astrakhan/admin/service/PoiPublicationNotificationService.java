package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.NewsSubscriber;
import ru.astrakhan.admin.entity.PointOfInterest;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PoiPublicationNotificationService {

    private final NewsletterService newsletterService;
    private final OrderEmailService orderEmailService;

    @Value("${app.module2.url:http://localhost:3000}")
    private String module2Url;

    @Value("${app.api.public.base-url:http://localhost:8080}")
    private String apiPublicBaseUrl;

    @Async("mailExecutor")
    public void sendNewPoiToSubscribersAsync(PointOfInterest poi) {
        try {
            List<NewsSubscriber> subs = newsletterService.findActiveSubscribers();
            if (subs.isEmpty()) {
                return;
            }
            String mapUrl = module2Url.replaceAll("/$", "") + "/";
            String snippet = shorten(poi.getDescription(), 220);
            String apiBase = apiPublicBaseUrl.replaceAll("/$", "");
            for (NewsSubscriber sub : subs) {
                String unsub = apiBase + "/api/v1/newsletter/unsubscribe?token=" + sub.getUnsubscribeToken();
                orderEmailService.sendNewPoiNewsletterEmail(sub.getEmail(), poi.getName(), snippet, mapUrl, unsub);
            }
            log.info("Newsletter: new POI «{}» queued to {} subscribers", poi.getName(), subs.size());
        } catch (Exception e) {
            log.error("Newsletter send failed for POI {}", poi.getId(), e);
        }
    }

    private static String shorten(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
