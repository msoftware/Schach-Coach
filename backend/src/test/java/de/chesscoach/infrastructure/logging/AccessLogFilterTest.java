package de.chesscoach.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLogFilterTest {

    @Test
    void clientAddressesAreShortenedBeforeLogging() {
        assertThat(AccessLogFilter.anonymize("203.0.113.57")).isEqualTo("203.0.113.0");
        assertThat(AccessLogFilter.anonymize("2001:db8:85a3:8d3:1319:8a2e:370:7348")).isEqualTo("2001:db8:85a3::");
        assertThat(AccessLogFilter.anonymize("::1")).isEqualTo("::");
        assertThat(AccessLogFilter.anonymize("2001:db8::1")).isEqualTo("2001:db8:0::");
        assertThat(AccessLogFilter.anonymize("0:0:0:0:0:0:0:1")).isEqualTo("::");
        assertThat(AccessLogFilter.anonymize(null)).isEqualTo("-");
    }

    @Test
    void requestIdsAreShortHex() {
        assertThat(AccessLogFilter.newRequestId()).matches("[0-9a-f]{8}");
        assertThat(AccessLogFilter.shortGameId("0123456789abcdef0123456789abcdef")).isEqualTo("01234567");
    }
}
