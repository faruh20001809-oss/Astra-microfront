package ru.astrakhan.admin.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/** Иконка для панели /java-api/admin (браузер часто запрашивает /favicon.ico с корня — там nginx). */
@RestController
public class FaviconController {

    @GetMapping({ "/favicon.ico", "/favicon.svg" })
    public ResponseEntity<byte[]> favicon() throws IOException {
        ClassPathResource res = new ClassPathResource("static/favicon.svg");
        byte[] body = res.getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(body);
    }
}
