package com.intelera.agentic.patient.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates MDC (Mapped Diagnostic Context) for every HTTP request.
 *
 * <p>MDC fields set per request: - requestId: from X-Request-ID header or a newly generated UUID -
 * traceId: from active micrometer tracing span - spanId: from active micrometer tracing span -
 * userId: principal name from SecurityContext, or "anonymous"
 *
 * <p>All MDC keys are cleared in a finally block to prevent context leakage across threads in a
 * thread-pool environment.
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

  private static final String MDC_REQUEST_ID = "requestId";
  private static final String MDC_TRACE_ID = "traceId";
  private static final String MDC_SPAN_ID = "spanId";
  private static final String MDC_USER_ID = "userId";
  private static final String HEADER_REQUEST_ID = "X-Request-ID";

  private final Tracer tracer;

  public MdcLoggingFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    try {
      populateMdc(request, response);
      chain.doFilter(request, response);
    } finally {
      // Always clear MDC to prevent context leakage to subsequent requests on this thread
      MDC.remove(MDC_REQUEST_ID);
      MDC.remove(MDC_TRACE_ID);
      MDC.remove(MDC_SPAN_ID);
      MDC.remove(MDC_USER_ID);
    }
  }

  private void populateMdc(HttpServletRequest request, HttpServletResponse response) {
    // Request ID: propagate from header or generate a new UUID
    String requestId =
        Optional.ofNullable(request.getHeader(HEADER_REQUEST_ID))
            .filter(id -> !id.isBlank())
            .orElse(UUID.randomUUID().toString());
    MDC.put(MDC_REQUEST_ID, requestId);
    response.setHeader(HEADER_REQUEST_ID, requestId);

    // Trace and Span IDs from the active micrometer tracing span
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
      MDC.put(MDC_TRACE_ID, currentSpan.context().traceId());
      MDC.put(MDC_SPAN_ID, currentSpan.context().spanId());
    } else {
      MDC.put(MDC_TRACE_ID, "no-trace");
      MDC.put(MDC_SPAN_ID, "no-span");
    }

    // User ID from Spring Security context
    String userId = resolveUserId();
    MDC.put(MDC_USER_ID, userId);
  }

  private String resolveUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return "anonymous";
    }
    return auth.getName();
  }
}
