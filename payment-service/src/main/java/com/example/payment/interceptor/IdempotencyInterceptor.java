package com.example.payment.interceptor;

import com.example.payment.domain.IdempotencyKey;
import com.example.payment.domain.KeyStatus;
import com.example.payment.filter.ResponseWrappingFilter;
import com.example.payment.repository.IdempotencyRepository;
import com.example.payment.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * @author a.komolov
 * @date 2026-04-06
 */
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String KEY_NAME = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
        if (HttpMethod.POST.equals(httpMethod)) {
            String key = request.getHeader(KEY_NAME);

            if (key == null || key.isEmpty()) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.getWriter().println(KEY_NAME + " header is empty");
                return false;
            }

            var existingKey = idempotencyService.getByKey(key);
            if  (existingKey.isPresent()) {
                if (KeyStatus.PENDING.equals(existingKey.get().getStatus())) {
                    response.setStatus(HttpStatus.CONFLICT.value());
                    response.getWriter().println(KEY_NAME + " already exists");
                } else if (KeyStatus.COMPLETED.equals(existingKey.get().getStatus())) {
                    response.setStatus(existingKey.get().getStatusCode());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().println(existingKey.get().getResponse());
                } else throw new IllegalArgumentException(KEY_NAME + " invalid status");
                return false;
            } else {
                try {
                    idempotencyService.createPendingKey(key);
                    return true;
                } catch (Exception e) {
                    response.setStatus(HttpStatus.CONFLICT.value());
                    response.getWriter().println("try later");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
        if (HttpMethod.POST.equals(httpMethod)) {
            // Получаем обёрнутый ответ из атрибута
            ContentCachingResponseWrapper wrappedResponse =
                    (ContentCachingResponseWrapper) request.getAttribute(ResponseWrappingFilter.WRAPPED_RESPONSE_ATTRIBUTE);
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), wrappedResponse.getCharacterEncoding());

            var key = request.getHeader(KEY_NAME);
            idempotencyService.markKeyAsCompleted(key, responseBody, response.getStatus());
        }
    }
}
