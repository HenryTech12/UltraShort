package org.app.UltraShort.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HashServiceTest {

    @Test
    void sameInputAlwaysProducesSameHash() {
        String url = "https://www.example.com/some/very/long/path?query=value";

        String first = HashService.generateHash(url);
        String second = HashService.generateHash(url);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        String hashA = HashService.generateHash("https://www.example.com/page-a");
        String hashB = HashService.generateHash("https://www.example.com/page-b");

        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    void hashIsNonNullNumericString() {
        String hash = HashService.generateHash("https://www.example.com");

        assertThat(hash).isNotBlank();
        assertThat(hash).matches("\\d+");
    }

    @Test
    void hashOfUnsignedIntNeverStartsWithMinusSign() {
        // murmur3_32 can produce negative ints; generateHash must render them unsigned
        for (int i = 0; i < 200; i++) {
            String hash = HashService.generateHash("input-" + i);
            assertThat(hash).doesNotStartWith("-");
        }
    }

    @Test
    void appendingTimestampChangesHash_collisionRegenerationStrategy() {
        String url = "https://www.example.com/collide";
        String original = HashService.generateHash(url);
        String regenerated = HashService.generateHash(url + System.currentTimeMillis());

        assertThat(regenerated).isNotEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a"})
    void handlesEdgeCaseInputsWithoutThrowing(String input) {
        assertThat(HashService.generateHash(input)).isNotBlank();
    }

    @Test
    void handlesVeryLongInputWithoutThrowing() {
        String longInput = "https://x.com/" + "y".repeat(2000);
        assertThat(HashService.generateHash(longInput)).isNotBlank();
    }
}
