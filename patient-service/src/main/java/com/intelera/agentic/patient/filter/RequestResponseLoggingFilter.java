package com.intelera.agentic.patient.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs incoming HTTP requests and outgoing response summaries.
 *
 * <p>Request body is NOT logged to avoid exposing sensitive patient data (PII, PHI). Only method,
 * URI, status code, and duration are recorded.
 *
 * <p>Actuator endpoints are excluded from logging to reduce operational noise.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    long startTime = System.currentTimeMillis();

    if (log.isDebugEnabled()) {
      log.debug(
          "Incoming request: method={}, uri={}, remoteAddr={}",
          request.getMethod(),
          request.getRequestURI(),
          request.getRemoteAddr());
    }

    try {
      chain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      log.info(
          "Request completed: method={}, uri={}, status={}, durationMs={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
    }
  }

  /** Skip detailed logging for actuator endpoints to reduce operational log noise. */
  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator");
  }
}
