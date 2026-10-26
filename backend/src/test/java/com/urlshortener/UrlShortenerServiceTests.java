package com.urlshortener;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class UrlShortenerServiceTests {

    private static final String BASE_URL = "http://localhost:8080";
    private UrlShortenerService service;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        var jdbcTemplate = new JdbcTemplate(dataSource);
        var repository = new ShortenedUrlRepository(jdbcTemplate);
        repository.init();
        service = new UrlShortenerService(repository);
        pageable = PageRequest.of(0, 5);
    }

    @Test
    void shorten_WithValidUrl_ReturnsShortUrlResponse() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/test");

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertNotNull(response.getAlias());
        assertEquals("https://example.com/test", response.getFullUrl());
        assertTrue(response.getShortUrl().startsWith(BASE_URL + "/"));
    }

    @Test
    void shorten_WithCustomAlias_ReturnsCustomAlias() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("my-alias");

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertEquals("my-alias", response.getAlias());
        assertEquals(BASE_URL + "/my-alias", response.getShortUrl());
    }

    @Test
    void whenShorten_withCustomAlias_TrimsAlias() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("  trimmed-alias  ");

        ShortenUrlResponse response = service.shorten(request, BASE_URL);

        assertEquals("trimmed-alias", response.getAlias());
        assertEquals(BASE_URL + "/trimmed-alias", response.getShortUrl());
    }

    @Test
    void whenShorten_withInvalidUrl_ThrowsIllegalArgumentException() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("not-a-url");
        request.setCustomAlias("invalid-url");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals("fullUrl must be a valid URL.", exception.getMessage());
    }

    @Test
    void whenShorten_withUnsupportedUrlScheme_ThrowsIllegalArgumentException() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("ftp://example.com/file");
        request.setCustomAlias("ftp-url");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals("fullUrl must be a valid URL.", exception.getMessage());
    }

    @Test
    void whenShorten_withBlankUrl_throwsIllegalArgumentException() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("   ");
        request.setCustomAlias("blank-url");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals("fullUrl is required.", exception.getMessage());
    }

    @Test
    void whenShorten_withInvalidAliasCharacters_throwsIllegalArgumentException() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("bad_alias!");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals("Alias may only contain letters, numbers, and hyphens (2–64 characters).",
                exception.getMessage());
    }

    @Test
    void whenShorten_withAliasTooShort_throwsIllegalArgumentException() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("a");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals("Alias may only contain letters, numbers, and hyphens (2–64 characters).",
                exception.getMessage());
    }

    @Test
    void whenShorten_withDuplicateAlias_throwsIllegalStateException() {
        var firstRequest = new ShortenUrlRequest();
        firstRequest.setFullUrl("https://example.com/first");
        firstRequest.setCustomAlias("duplicate");

        var secondRequest = new ShortenUrlRequest();
        secondRequest.setFullUrl("https://example.com/second");
        secondRequest.setCustomAlias("duplicate");

        service.shorten(firstRequest, BASE_URL);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.shorten(secondRequest, BASE_URL)
        );

        assertEquals("The alias 'duplicate' is already taken.", exception.getMessage());
    }

    @Test
    void getFullUrl_ReturnsExistingUrl() throws ExecutionException, InterruptedException {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com");
        request.setCustomAlias("example");
        service.shorten(request, BASE_URL);

        String fullUrl = CompletableFuture.supplyAsync(() -> service.getFullUrl("example")).get();

        assertEquals("https://example.com/", fullUrl);
    }

    @Test
    void whenDelete_withExistingAlias_removesUrlAndReturnsTrue() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/delete-me");
        request.setCustomAlias("delete-me");
        service.shorten(request, BASE_URL);

        boolean deleted = service.delete("delete-me");

        assertTrue(deleted);
        assertNull(service.getFullUrl("delete-me"));
    }

    @Test
    void whenDelete_withMissingAlias_returnsFalse() {
        boolean deleted = service.delete("missing");
        assertFalse(deleted);
    }

    @Test
    void whenGetFullUrl_withCorrectAlias_returnString() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/found");
        request.setCustomAlias("find-me");
        service.shorten(request, BASE_URL);

        var response = service.getFullUrl("find-me");
        assertNotNull(response);
    }

    @Test
    void whenGetFullUrl_withIncorrectAlias_returnNull() {
        var response = service.getFullUrl("not-found");
        assertNull(response);
    }

    @Test
    void whenGetAll_withBaseUrl_returnList() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/item-1");
        request.setCustomAlias("Item-1");
        service.shorten(request, BASE_URL);

        var response = service.getAll(BASE_URL, pageable);
        assertNotNull(response);
    }

    @Test
    void whenGetAll_withNegativePage_returnError() {
        // need to implement
    }

    @Test
    void whenGetAll_withOver10_returnError() {
        // need to implement
    }

    @Test
    void whenGetAll_withNoList_returnEmptyList() {
        // need to implement
    }
}
