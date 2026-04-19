package com.example.order.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author a.komolov
 * @date 2026-04-19
 */
@Slf4j
@Component
@Order(value = 1)
public class RateLimiterFilter implements Filter {

    private final ConcurrentHashMap<String, Bucket> ipAddressToBucketMap = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String remoteAddress = request.getRemoteAddr();
        var bucket = ipAddressToBucketMap.computeIfAbsent(remoteAddress, this::createBucket);
        log.info("AvailableTokens for IP {} is {}", remoteAddress, bucket.getAvailableTokens());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("TOO MANY REQUESTS");
        }
    }

    private Bucket createBucket(String remoteAddress) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(2)
                .refillIntervally(1, Duration.ofSeconds(5))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
