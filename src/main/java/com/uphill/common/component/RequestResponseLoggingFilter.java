package com.uphill.common.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 0);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            String requestBody = getPayload(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
            log.info("--- HTTP REQUEST --- Method: {} / URI: {} / Payload: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    requestBody.isEmpty() ? "[empty]" : requestBody);
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            stopWatch.stop();
            String responseBody = getPayload(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());
            log.info("--- HTTP RESPONSE --- Status: {} / Duration: {} ms / Payload: {}",
                    response.getStatus(),
                    stopWatch.getTotalTimeMillis(),
                    responseBody.isEmpty() ? "[empty]" : responseBody
            );

            responseWrapper.copyBodyToResponse();
        }
    }

    private String getPayload(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        try {
            return new String(buf, encoding != null ? encoding : StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return "[unparseable binary data]";
        }
    }
}
