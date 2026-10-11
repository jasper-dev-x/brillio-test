package com.listings.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.listings.search.model.Listing;
import com.listings.search.model.ListingStatus;

class JsonFileListingRepositoryTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void loadsTheSampleDataset() {
        JsonFileListingRepository repository =
                new JsonFileListingRepository(new ClassPathResource("data/sample_listings.json"), objectMapper);

        List<Listing> listings = repository.findAll();

        assertThat(listings).hasSize(12);
        Listing first = listings.get(0);
        assertThat(first.key()).isEqualTo("MLS_A:A1");
        assertThat(first.price()).isEqualByComparingTo(BigDecimal.valueOf(450_000));
        assertThat(first.bathrooms()).isEqualTo(1.5);
        assertThat(first.listedDate()).isEqualTo(LocalDate.of(2026, 8, 29));
        assertThat(first.status()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(listings.stream().filter(l -> l.status() == ListingStatus.PENDING).count()).isEqualTo(1L);
    }

    @Test
    void failsFastWhenTheDataFileIsMissing() {
        assertThatThrownBy(() -> new JsonFileListingRepository(new ClassPathResource("data/missing.json"), objectMapper))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("missing.json");
    }
}
