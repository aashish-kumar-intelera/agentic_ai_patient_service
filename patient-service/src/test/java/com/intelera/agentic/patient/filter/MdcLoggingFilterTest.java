package com.intelera.agentic.patient.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class MdcLoggingFilterTest {

  @Mock private Tracer tracer;

  @Mock private Span span;

  @Mock private TraceContext traceContext;

  private MdcLoggingFilter filter;

  @BeforeEach
  void setUp() {
    filter = new MdcLoggingFilter(tracer);
  }

  @Test
  void doFilterInternal_whenRequestCompletes_shouldClearMdcAfter() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("abc-trace");
    when(traceContext.spanId()).thenReturn("abc-span");

    // Act
    filter.doFilterInternal(request, response, chain);

    // Assert — MDC must be cleared after filter completes
    assertThat(MDC.get("requestId")).isNull();
    assertThat(MDC.get("traceId")).isNull();
    assertThat(MDC.get("spanId")).isNull();
    assertThat(MDC.get("userId")).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_whenXRequestIdProvided_shouldUseItAndPropagateToResponse()
      throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "my-custom-request-id");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(tracer.currentSpan()).thenReturn(null);

    final String[] capturedRequestId = {null};
    doAnswer(
            invocation -> {
              capturedRequestId[0] = MDC.get("requestId");
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    // Act
    filter.doFilterInternal(request, response, chain);

    // Assert — custom header value used, not a generated UUID
    assertThat(capturedRequestId[0]).isEqualTo("my-custom-request-id");
    assertThat(response.getHeader("X-Request-ID")).isEqualTo("my-custom-request-id");
  }

  @Test
  void doFilterInternal_whenNoXRequestIdHeader_shouldGenerateUuid() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(tracer.currentSpan()).thenReturn(null);

    final String[] capturedRequestId = {null};
    doAnswer(
            invocation -> {
              capturedRequestId[0] = MDC.get("requestId");
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    // Act
    filter.doFilterInternal(request, response, chain);

    // Assert — a UUID is generated when no header is present
    assertThat(capturedRequestId[0]).isNotNull().isNotBlank();
    // UUID format: 8-4-4-4-12 hex characters
    assertThat(capturedRequestId[0]).matches("[0-9a-f-]{36}");
  }

  @Test
  void doFilterInternal_whenActiveSpan_shouldPopulateTraceIdAndSpanId() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(tracer.currentSpan()).thenReturn(span);
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("expected-trace-id");
    when(traceContext.spanId()).thenReturn("expected-span-id");

    final String[] capturedTraceId = {null};
    final String[] capturedSpanId = {null};
    doAnswer(
            invocation -> {
              capturedTraceId[0] = MDC.get("traceId");
              capturedSpanId[0] = MDC.get("spanId");
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    // Act
    filter.doFilterInternal(request, response, chain);

    // Assert
    assertThat(capturedTraceId[0]).isEqualTo("expected-trace-id");
    assertThat(capturedSpanId[0]).isEqualTo("expected-span-id");
  }

  @Test
  void doFilterInternal_whenNoActiveSpan_shouldSetNoTraceValues() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    when(tracer.currentSpan()).thenReturn(null);

    final String[] capturedTraceId = {null};
    doAnswer(
            invocation -> {
              capturedTraceId[0] = MDC.get("traceId");
              return null;
            })
        .when(chain)
        .doFilter(any(), any());

    // Act
    filter.doFilterInternal(request, response, chain);

    // Assert — fallback value set when no active span
    assertThat(capturedTraceId[0]).isEqualTo("no-trace");
  }
}
