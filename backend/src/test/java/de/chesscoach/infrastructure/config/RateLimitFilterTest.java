package de.chesscoach.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static RateLimitFilter filter(int perGame, int perClient) {
        return new RateLimitFilter(new ApiProperties(List.of(), 500, Duration.ofHours(6), perGame, 600, perClient,
                Duration.ofMinutes(30)));
    }

    private static int post(RateLimitFilter filter, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void playersBehindTheSameAddressHaveIndependentLimits() throws Exception {
        RateLimitFilter filter = filter(2, 100);
        String gameA = "/api/games/" + "a".repeat(32) + "/moves";
        String gameB = "/api/games/" + "b".repeat(32) + "/moves";
        assertThat(post(filter, gameA, "10.0.0.1")).isEqualTo(200);
        assertThat(post(filter, gameA, "10.0.0.1")).isEqualTo(200);
        assertThat(post(filter, gameA, "10.0.0.1")).isEqualTo(429);
        assertThat(post(filter, gameB, "10.0.0.1")).isEqualTo(200);
    }

    @Test
    void addressLimitStillAppliesAcrossGames() throws Exception {
        RateLimitFilter filter = filter(100, 2);
        assertThat(post(filter, "/api/games", "10.0.0.2")).isEqualTo(200);
        assertThat(post(filter, "/api/games/" + "c".repeat(32) + "/hint", "10.0.0.2")).isEqualTo(200);
        assertThat(post(filter, "/api/games", "10.0.0.2")).isEqualTo(429);
        assertThat(post(filter, "/api/games", "10.0.0.3")).isEqualTo(200);
    }

    @Test
    void concurrentRequestsOnOneGameNeverExceedItsLimit() throws Exception {
        RateLimitFilter filter = filter(100, 0);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < 50; i++) {
                    if (filter.tryAcquire("game:shared", 100)) {
                        allowed.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(1, TimeUnit.MINUTES);
        }
        pool.shutdown();
        // 400 Versuche, 100 Token; ein weiteres Token kann höchstens bei sehr langsamer Ausführung nachfließen.
        assertThat(allowed.get()).isBetween(100, 101);
    }

    @Test
    void floodOfNewKeysDoesNotResetExistingLimits() {
        RateLimitFilter filter = filter(2, 2);
        assertThat(filter.tryAcquire("ip:attacker", 2)).isTrue();
        assertThat(filter.tryAcquire("ip:attacker", 2)).isTrue();
        assertThat(filter.tryAcquire("ip:attacker", 2)).isFalse();

        for (int i = 0; i <= RateLimitFilter.MAX_BUCKETS + 5; i++) {
            filter.tryAcquire("ip:client-" + i, 2);
        }

        assertThat(filter.tryAcquire("ip:attacker", 2)).isFalse();
    }
}
