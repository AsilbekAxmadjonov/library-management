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

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String OPEN_LIBRARY_URL =
            "https://openlibrary.org/api/books" +
                    "?bibkeys=ISBN:{isbn}&format=json&jscmd=data";

    @Override
    public OpenLibraryBookDto fetchByIsbn(String isbn) {
        try {
            log.info("Fetching book data for ISBN: {}", isbn);

            String responseJson = restClient.get()
                    .uri(OPEN_LIBRARY_URL, isbn)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.equals("{}")) {
                log.warn("No book found for ISBN: {}", isbn);
                return null;
            }

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
