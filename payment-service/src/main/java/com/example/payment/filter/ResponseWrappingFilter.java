package com.example.payment.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ResponseWrappingFilter implements Filter {

    public static final String WRAPPED_RESPONSE_ATTRIBUTE = "wrappedResponse";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Оборачиваем response
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        // Сохраняем обёртку в атрибут запроса
        httpRequest.setAttribute(WRAPPED_RESPONSE_ATTRIBUTE, wrappedResponse);

        try {
            chain.doFilter(request, wrappedResponse);
        } finally {
            // Копируем тело ответа обратно
            wrappedResponse.copyBodyToResponse();
        }
    }
}