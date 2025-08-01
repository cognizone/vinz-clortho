package cogni.zone.vinzclortho;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Context;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.api.Assertions;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@ActiveProfiles("test-spelHeader")
class SpelHeaderTest extends GoVinzTest {

  private static final MemoryAppender memoryAppender = new MemoryAppender();

  @BeforeAll //beforeAll needs to be static
  static void beforeAll() {
    //no logback means kablemo
    Logger logger = (Logger) LoggerFactory.getLogger(VinzClorthoFilter.class);
    logger.addAppender(memoryAppender);

    memoryAppender.setContext((Context) LoggerFactory.getILoggerFactory());
    memoryAppender.start();
  }

  @BeforeEach
  void beforeEach() {
    memoryAppender.reset();
  }

  @Test
  void testSpelRequestHeader() throws Exception {
    HttpClient httpClient = setupMocks();

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/spelHeader/test")
            .servletPath("/proxy/spelHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("X-Static-Response-Header", "ResponseOne#{T(java.time.LocalDateTime).now().toString()}"))
           .andExpect(MockMvcResultMatchers.header().string("X-Spel-Response-Header", new CheckNonOldDateTimeMatcher("ResponseOne")));

    AtomicBoolean requestChecked = new AtomicBoolean(false);
    verify(httpClient).execute(argThat(request -> {
      if (requestChecked.getAndSet(true)) return true; // Only check once

      HttpRequestBase httpRequest = (HttpRequestBase) request;

      // Verify static request header
      Header staticHeader = httpRequest.getFirstHeader("X-Static-Request-Header");
      Assertions.assertThat(staticHeader).isNotNull();
      Assertions.assertThat(staticHeader.getValue()).isEqualTo("RequestOne#{T(java.time.LocalDateTime).now().toString()}");

      // Verify SpEL request header
      Header spelHeader = httpRequest.getFirstHeader("X-Spel-Request-Header");
      Assertions.assertThat(spelHeader).isNotNull();
      MatcherAssert.assertThat(spelHeader.getValue(), new CheckNonOldDateTimeMatcher("RequestOne"));

      // Verify SpEL request header with value from environment
      Header spelHeaderFromEnvironment = httpRequest.getFirstHeader("X-From-Environment-WithLog");
      Assertions.assertThat(spelHeaderFromEnvironment).isNotNull();
      Assertions.assertThat(spelHeaderFromEnvironment.getValue()).isEqualTo("Hi - hello");

      // Verify SpEL request header with value from environment
      Header spelHeaderFromEnvironmentNoLog = httpRequest.getFirstHeader("X-From-Environment-NoLog");
      Assertions.assertThat(spelHeaderFromEnvironmentNoLog).isNotNull();
      Assertions.assertThat(spelHeaderFromEnvironmentNoLog.getValue()).isEqualTo("Hi - hello");

      return true;
    }));

    Assertions.assertThat(requestChecked).isTrue();

    //check what is logged, important for spel vs spelNoLog
    boolean hasWithLog = memoryAppender.hasContainsAll("Patched header [X-From-Environment-WithLog] : from [Hi - #{@environment.getProperty('we.have.a.property')}] to [Hi - hello]");
    Assertions.assertThat(hasWithLog).isTrue();

    boolean hasNoLog = memoryAppender.hasContainsAll("Patched header [X-From-Environment-NoLog]");
    Assertions.assertThat(hasNoLog).isFalse();

    long logCount = memoryAppender.countContainsAll("Patched header");
    Assertions.assertThat(logCount).isEqualTo(3L);
  }

  @RequiredArgsConstructor
  private static class CheckNonOldDateTimeMatcher extends BaseMatcher<String> {
    private final String prefix;

    @Override
    public boolean matches(Object actual) {
      if (!(actual instanceof String value)) return false;

      if (!value.startsWith(prefix)) return false;

      long headerMs = LocalDateTime.parse(value.substring(prefix.length())).toEpochSecond(ZoneOffset.UTC);
      long nowMs = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

      long diff = nowMs - headerMs;
      return diff < TimeUnit.MINUTES.toMillis(1L); //let's go for max 1 minute between the test execution and the validation
    }

    @Override
    public void describeTo(Description description) {
    }
  }

  @SneakyThrows
  private HttpClient setupMocks() {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity httpEntity = mock(HttpEntity.class);

    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(statusLine.getStatusCode()).thenReturn(200);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("IT".getBytes(StandardCharsets.UTF_8)));

    return httpClient;
  }
}
