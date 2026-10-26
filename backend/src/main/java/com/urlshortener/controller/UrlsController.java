package com.urlshortener.controller;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import com.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.net.URI;
import java.util.List;

@RestController
@org.springframework.web.bind.annotation.CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UrlsController {

    private final UrlShortenerService service;

    public UrlsController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponse> shorten(
            @Valid @RequestBody ShortenUrlRequest request,
            HttpServletRequest requestContext) {

        var baseUrl = getBaseUrl(requestContext);
        var shortenedUrl = service.shorten(request, baseUrl);

        return ResponseEntity
                .created(URI.create(shortenedUrl.getShortUrl()))
                .body(shortenedUrl);
    }

    @GetMapping("/urls")
    public List<UrlListItem> getAll(
            @PageableDefault(size = 5) Pageable pageable,
            HttpServletRequest requestContext) {
        var baseUrl = getBaseUrl(requestContext);
        return service.getAll(baseUrl, pageable);
    }

    @GetMapping("/{alias}")
    public ResponseEntity<Void> redirectToUrl(@PathVariable String alias) {
        var fullUrl = service.getFullUrl(alias);
        if (fullUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(fullUrl)).build();
    }

    // Note: I would change this to a delete e.g. /urls/{alias} so the /{alias} isn't root level
    // I kept it like this as there could be a business reason for this setup but would suggest above
    @DeleteMapping("/{alias}")
    public ResponseEntity<Void> delete(@PathVariable String alias) {
        var deleted = service.delete(alias);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private String getBaseUrl(HttpServletRequest request) {
        var scheme = request.getScheme();
        var serverName = request.getServerName();
        var serverPort = request.getServerPort();
        var baseUrl = new StringBuilder(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            baseUrl.append(":").append(serverPort);
        }
        return baseUrl.toString();
    }
}
