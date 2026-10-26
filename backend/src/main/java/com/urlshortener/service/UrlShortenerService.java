package com.urlshortener.service;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import com.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class UrlShortenerService {

    private static final String ALIAS_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int GENERATED_ALIAS_LENGTH = 7;
    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 5;

    private final ShortenedUrlRepository repository;

    public UrlShortenerService(ShortenedUrlRepository repository) {
        this.repository = repository;
    }

    public ShortenUrlResponse shorten(ShortenUrlRequest request, String baseUrl) {
        var fullUrl = normalizeUrl(request.getFullUrl());
        var alias = request.getCustomAlias() == null || request.getCustomAlias().isBlank()
                ? generateAlias()
                : request.getCustomAlias().trim();

        if (!isValidAlias(alias)) {
            throw new IllegalArgumentException("Alias may only contain letters, numbers, and hyphens (2–64 characters).");
        }

        if (repository.existsByAlias(alias)) {
            throw new IllegalStateException("The alias '" + alias + "' is already taken.");
        }

        repository.save(alias, fullUrl, Instant.now());

        return new ShortenUrlResponse(baseUrl + "/" + alias, alias, fullUrl);
    }

    public String getFullUrl(String alias) {
        return repository.findFullUrlByAlias(alias).orElse(null);
    }

    public List<UrlListItem> getAll(String baseUrl, Pageable pageable) {
        return repository.findAll(baseUrl, pageable);
    }

    public boolean delete(String alias) {
        if (repository.findFullUrlByAlias(alias).orElse(null) == null) {
            return false;
        }
        return repository.deleteByAlias(alias);
    }

    private String generateAlias() {
        // Generate a random alias using characters from ALIAS_CHARS.
        // If the alias is already taken, retry up to 5 times before failing.
        // Note for test: I did think about using UUID, but for this case on shortening the URL, it was better this way.
        // Also I did 5 retries, but for future improvement, consider increasing the number of retries depending on the load.
        for (int attempt = 0; attempt < MAX_ALIAS_GENERATION_ATTEMPTS; attempt++) {
            var alias = ThreadLocalRandom.current()
                    .ints(GENERATED_ALIAS_LENGTH, 0, ALIAS_CHARS.length())
                    .mapToObj(ALIAS_CHARS::charAt)
                    .map(String::valueOf)
                    .collect(Collectors.joining());
            if (!repository.existsByAlias(alias)) {
                return alias;
            }
        }
        throw new IllegalStateException("Unable to generate a unique alias.");
    }

    private static boolean isValidAlias(String alias) {
        if (alias == null || alias.length() < 2 || alias.length() > 64) {
            return false;
        }
        return alias.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-');
    }

    private static String normalizeUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) {
            throw new IllegalArgumentException("fullUrl is required.");
        }

        try {
            var uri = new URI(fullUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    path,
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("fullUrl must be a valid URL.");
        }
    }
}
