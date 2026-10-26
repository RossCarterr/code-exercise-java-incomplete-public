package com.urlshortener;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC"
})
class UrlsControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM shortened_urls");
        jdbcTemplate.update(
                "INSERT INTO shortened_urls (alias, full_url, created_at) VALUES (?, ?, ?)",
                "redir",
                "https://target.com/",
                Instant.now().toString()
        );
    }

    @Test
    void getAlias_ExistingAlias_Returns302WithLocation() {
        var response = restTemplate.getForEntity("/redir", String.class);

        assertThat(response.getStatusCode()).isEqualTo(FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://target.com/");
    }

    @Test
    void getAlias_withMissingAlias_Returns404() {
        var response = restTemplate.getForEntity("/not-found", String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void whenShorten_withCustomAlias_Returns201WithResponseShortenUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/test");
        request.setCustomAlias("test");

        ResponseEntity<ShortenUrlResponse> response = restTemplate.postForEntity("/shorten", request, ShortenUrlResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAlias()).isEqualTo("test");
        assertThat(response.getBody().getFullUrl()).isEqualTo("https://example.com/test");
        assertThat(response.getBody().getShortUrl()).endsWith("/test");
        assertThat(response.getHeaders().getLocation()).hasToString(response.getBody().getShortUrl());
    }

    @Test
    void whenShorten_withoutCustomAlias_Returns201WithResponseShortenUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/test");

        ResponseEntity<ShortenUrlResponse> response = restTemplate.postForEntity("/shorten", request, ShortenUrlResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFullUrl()).isEqualTo("https://example.com/test");
        assertThat(response.getBody().getShortUrl()).endsWith(response.getBody().getAlias());
        assertThat(response.getHeaders().getLocation()).hasToString(response.getBody().getShortUrl());
    }

    @Test
    void whenShorten_withDuplicateAlias_Returns400WithDuplicateError() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/test");
        request.setCustomAlias("test");

        restTemplate.postForEntity("/shorten", request, ShortenUrlResponse.class); //First request
        ResponseEntity<ShortenUrlResponse> errorResponse = restTemplate.postForEntity("/shorten", request, ShortenUrlResponse.class);

        assertThat(errorResponse.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void whenShorten_withInvalidAlias_Returns400() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setFullUrl("https://example.com/test");
        request.setCustomAlias("bad_alias!");

        ResponseEntity<String> response = restTemplate.postForEntity("/shorten", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).contains("Alias may only contain");
    }

    @Test
    void whenDeleteAlias_withExistingAlias_returns204AndRemovesUrl() {
        var deleteResponse = restTemplate.exchange(
                "/redir",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(NO_CONTENT);

        var getResponse = restTemplate.getForEntity("/redir", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void whenDeleteAlias_withMissingAlias_returns404() {
        var response = restTemplate.exchange(
                "/missing",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void whenGetAll_withList_returns200AndAllUrls() {
        var response = restTemplate.getForEntity("/urls", String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void whenGetAll_withNoList_returns200withEmptyList() {
        jdbcTemplate.execute("DELETE FROM shortened_urls");
        var response = restTemplate.getForEntity("/urls", String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void whenRedirectToUrl_withAlias_returns302withShortUrl() {
        var response = restTemplate.getForEntity("/redir", String.class);
        assertThat(response.getStatusCode()).isEqualTo(FOUND);
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void whenRedirectToUrl_withIncorrectAlias_returns404withShortUrl() {
        var response = restTemplate.getForEntity("/not-found", String.class);
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }
}
