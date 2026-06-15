package com.library.management.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryBookDto(

        String title,

        List<AuthorDto> authors,

        @JsonProperty("publish_date")
        String publishDate,

        @JsonProperty("number_of_pages")
        Integer numberOfPages,

        List<PublisherDto> publishers
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorDto(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PublisherDto(String name) {}
}
