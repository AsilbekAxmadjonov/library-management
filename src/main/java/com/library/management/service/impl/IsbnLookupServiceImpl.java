package com.library.management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.external.OpenLibraryBookDto;
import com.library.management.service.IsbnLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IsbnLookupServiceImpl implements IsbnLookupService {

    // RestClient is Spring Boot 3.2+'s modern HTTP client
    // We configure it in a @Bean — injected here
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String OPEN_LIBRARY_URL =
            "https://openlibrary.org/api/books" +
                    "?bibkeys=ISBN:{isbn}&format=json&jscmd=data";

    @Override
    public OpenLibraryBookDto fetchByIsbn(String isbn) {
        try {
            log.info("Fetching book data for ISBN: {}", isbn);

            // Call Open Library API
            // Response is a Map where the key is "ISBN:978-xxx"
            // and the value is the book data object
            String responseJson = restClient.get()
                    .uri(OPEN_LIBRARY_URL, isbn)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.equals("{}")) {
                log.warn("No book found for ISBN: {}", isbn);
                return null;
            }

            // Parse the outer map — key is "ISBN:{isbn}"
            Map<String, Object> responseMap = objectMapper.readValue(
                    responseJson,
                    objectMapper.getTypeFactory()
                            .constructMapType(Map.class, String.class, Object.class)
            );

            String key = "ISBN:" + isbn;
            if (!responseMap.containsKey(key)) {
                log.warn("ISBN key not found in response: {}", isbn);
                return null;
            }

            // Convert the inner object to our DTO
            Object bookData = responseMap.get(key);
            String bookJson = objectMapper.writeValueAsString(bookData);
            OpenLibraryBookDto dto = objectMapper.readValue(
                    bookJson, OpenLibraryBookDto.class);

            log.info("Successfully fetched book: title='{}' for ISBN: {}",
                    dto.title(), isbn);
            return dto;

        } catch (RestClientException e) {
            log.error("HTTP error fetching ISBN {}: {}", isbn, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Failed to reach Open Library API. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Error parsing response for ISBN {}: {}", isbn, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Failed to parse book data from Open Library.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
