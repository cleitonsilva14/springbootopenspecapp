package io.dev.openspec.springbootopenspecapp.common;

import io.dev.openspec.springbootopenspecapp.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void copiesPageMetadata() {
        var page = new PageImpl<>(List.of("a", "b", "c", "d", "e"), PageRequest.of(1, 20), 25);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b", "c", "d", "e");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.totalPages()).isEqualTo(2);
    }
}
