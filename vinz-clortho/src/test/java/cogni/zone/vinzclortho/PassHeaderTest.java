package cogni.zone.vinzclortho;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.message.BasicHeader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@ActiveProfiles("test-passHeader")
class PassHeaderTest extends GoVinzTest {

  @Test
  public void requestPass_cookieHeaderIsPassedToProxiedRequest() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String cookieValue = "sessionId=abc123; userId=456";
    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test")
            .header("Cookie", cookieValue);
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk());

    AtomicBoolean realRequestChecked = new AtomicBoolean(false);
    verify(httpClient).execute(argThat(realRequest -> {
      if (realRequestChecked.getAndSet(true)) return true;
      Assertions.assertThat(realRequest).isExactlyInstanceOf(HttpPost.class);

      Header cookieHeader = realRequest.getFirstHeader("Cookie");
      Assertions.assertThat(cookieHeader).isNotNull();
      Assertions.assertThat(cookieHeader.getValue()).isEqualTo(cookieValue);

      return true;
    }));
  }

  @Test
  public void requestPass_customHeaderIsPassedToProxiedRequest() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String customHeaderValue = "custom-value-123";
    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test")
            .header("X-Custom-Request-Header", customHeaderValue);
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk());

    AtomicBoolean realRequestChecked = new AtomicBoolean(false);
    verify(httpClient).execute(argThat(realRequest -> {
      if (realRequestChecked.getAndSet(true)) return true;
      Assertions.assertThat(realRequest).isExactlyInstanceOf(HttpPost.class);

      Header customHeader = realRequest.getFirstHeader("X-Custom-Request-Header");
      Assertions.assertThat(customHeader).isNotNull();
      Assertions.assertThat(customHeader.getValue()).isEqualTo(customHeaderValue);

      return true;
    }));
  }

  @Test
  public void requestPass_headerNotInConfigIsNotPassed() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test")
            .header("X-Not-Configured-Header", "should-not-pass");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk());

    AtomicBoolean realRequestChecked = new AtomicBoolean(false);
    verify(httpClient).execute(argThat(realRequest -> {
      if (realRequestChecked.getAndSet(true)) return true;
      Assertions.assertThat(realRequest).isExactlyInstanceOf(HttpPost.class);

      Header notConfiguredHeader = realRequest.getFirstHeader("X-Not-Configured-Header");
      Assertions.assertThat(notConfiguredHeader).isNull();

      return true;
    }));
  }

  @Test
  public void responsePass_setCookieHeaderIsPassedToResponse() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String setCookieValue = "sessionId=xyz789; Path=/; HttpOnly";
    when(httpResponse.getHeaders("Content-Type")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("Set-Cookie")).thenReturn(new Header[]{
            new BasicHeader("Set-Cookie", setCookieValue)
    });
    when(httpResponse.getHeaders("X-Custom-Response-Header")).thenReturn(new Header[0]);

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("Set-Cookie", setCookieValue));
  }

  @Test
  public void responsePass_customResponseHeaderIsPassedToResponse() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String customResponseValue = "custom-response-value";
    when(httpResponse.getHeaders("Content-Type")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("Set-Cookie")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("X-Custom-Response-Header")).thenReturn(new Header[]{
            new BasicHeader("X-Custom-Response-Header", customResponseValue)
    });

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("X-Custom-Response-Header", customResponseValue));
  }

  @Test
  public void responsePass_multipleSetCookieHeadersAreAllPassed() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String setCookie1 = "sessionId=abc; Path=/";
    String setCookie2 = "userId=123; Path=/";
    when(httpResponse.getHeaders("Content-Type")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("Set-Cookie")).thenReturn(new Header[]{
            new BasicHeader("Set-Cookie", setCookie1),
            new BasicHeader("Set-Cookie", setCookie2)
    });
    when(httpResponse.getHeaders("X-Custom-Response-Header")).thenReturn(new Header[0]);

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().stringValues("Set-Cookie", setCookie1, setCookie2));
  }

  @Test
  public void responsePass_onlyConfiguredHeadersArePassed() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String setCookieValue = "sessionId=xyz; Path=/";
    String customHeaderValue = "custom-value";
    String notConfiguredValue = "should-not-reach-caller";
    when(httpResponse.getHeaders("Content-Type")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("Set-Cookie")).thenReturn(new Header[]{
            new BasicHeader("Set-Cookie", setCookieValue)
    });
    when(httpResponse.getHeaders("X-Custom-Response-Header")).thenReturn(new Header[]{
            new BasicHeader("X-Custom-Response-Header", customHeaderValue)
    });
    when(httpResponse.getHeaders("X-Not-Configured-Response")).thenReturn(new Header[]{
            new BasicHeader("X-Not-Configured-Response", notConfiguredValue)
    });

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("Set-Cookie", setCookieValue))
           .andExpect(MockMvcResultMatchers.header().string("X-Custom-Response-Header", customHeaderValue))
           .andExpect(MockMvcResultMatchers.header().doesNotExist("X-Not-Configured-Response"));
  }

  @Test
  public void requestPass_responsePassHeaderIsNotPassedToRequest() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String cookieValue = "sessionId=abc123";
    String setCookieValue = "newSession=xyz";
    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test")
            .header("Cookie", cookieValue)
            .header("Set-Cookie", setCookieValue);
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk());

    AtomicBoolean realRequestChecked = new AtomicBoolean(false);
    verify(httpClient).execute(argThat(realRequest -> {
      if (realRequestChecked.getAndSet(true)) return true;

      Header cookieHeader = realRequest.getFirstHeader("Cookie");
      Assertions.assertThat(cookieHeader).isNotNull();
      Assertions.assertThat(cookieHeader.getValue()).isEqualTo(cookieValue);

      Header setCookieHeader = realRequest.getFirstHeader("Set-Cookie");
      Assertions.assertThat(setCookieHeader).isNull();

      return true;
    }));
  }

  @Test
  public void responsePass_requestPassHeaderIsNotPassedToResponse() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8)));

    String setCookieValue = "sessionId=xyz; Path=/";
    String cookieValue = "should-not-be-in-response";
    when(httpResponse.getHeaders("Content-Type")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("Set-Cookie")).thenReturn(new Header[]{
            new BasicHeader("Set-Cookie", setCookieValue)
    });
    when(httpResponse.getHeaders("X-Custom-Response-Header")).thenReturn(new Header[0]);
    when(httpResponse.getHeaders("Cookie")).thenReturn(new Header[]{
            new BasicHeader("Cookie", cookieValue)
    });

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/passHeader/test")
            .servletPath("/proxy/passHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("Set-Cookie", setCookieValue))
           .andExpect(MockMvcResultMatchers.header().doesNotExist("Cookie"));
  }
}
