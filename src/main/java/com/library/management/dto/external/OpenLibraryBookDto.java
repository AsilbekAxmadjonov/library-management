package com.library.management.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// @JsonIgnoreProperties(ignoreUnknown = true) — Open Library returns
// many fields we don't need, this annotation tells Jackson to ignore
// any field not declared here instead of throwing an error
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryBookDto(

        String title,

        // "authors": [{"name": "Robert C. Martin"}]
        List<AuthorDto> authors,

        // "publish_date": "2008" or "August 1, 2008"
        @JsonProperty("publish_date")
        String publishDate,

        // "number_of_pages": 431
        @JsonProperty("number_of_pages")
        Integer numberOfPages,

        // "publishers": [{"name": "Prentice Hall"}]
        List<PublisherDto> publishers
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorDto(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PublisherDto(String name) {}
}
