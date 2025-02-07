package cogni.zone.vinzclortho;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
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
@ActiveProfiles("test-addHeader")
class HeaderTest extends GoVinzTest {


  @Test
  public void headerTest() throws Exception {
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

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/addHeader/test")
            .servletPath("/proxy/addHeader/test");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("X-Bla1", "value1"))
           .andExpect(MockMvcResultMatchers.header().doesNotExist("X-Bla2")); //we don't have X-Bla-in, so no X-Bla2


  }

  @Test
  public void headerTest2() throws Exception {
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


    MockHttpServletRequestBuilder testPostRequest = post("/proxy/addHeader/test")
            .servletPath("/proxy/addHeader/test")
            .header("X-Bla-in", "bla");  //config says, if we have in header X-Bla-in, then return X-Bla2
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("X-Bla1", "value1"))
           .andExpect(MockMvcResultMatchers.header().string("X-Bla2", "value2"));
  }

  @Test
  public void header_setOverwrites() throws Exception {
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


    MockHttpServletRequestBuilder testPostRequest = post("/proxy/addHeader/test")
            .servletPath("/proxy/addHeader/test")
            .header("X-Bla-in", "bla")  //config says, if we have in header X-Bla-in, then return X-Bla2
            .header("X-Send-This-As-Header", "thisShouldBeOverwritten")
            .header("User-Agent", "broumbroumserver");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(MockMvcResultMatchers.header().string("X-Bla1", "value1"))
           .andExpect(MockMvcResultMatchers.header().string("X-Bla2", "value2"));

    AtomicBoolean realRequestBodyChecked = new AtomicBoolean(false);
    verify(httpClient).execute(argThat(thaRealRequest -> {
      if (realRequestBodyChecked.getAndSet(true)) return true; //called twice for some reason, just check once
      Assertions.assertThat(thaRealRequest).isExactlyInstanceOf(HttpPost.class);

      Header firstHeader = thaRealRequest.getFirstHeader("X-Send-This-As-Header");
      Assertions.assertThat(firstHeader).isNotNull();
      Assertions.assertThat(firstHeader.getValue()).isEqualTo("someValue");

      Header agentHeader = thaRealRequest.getFirstHeader("User-Agent");
      Assertions.assertThat(agentHeader).isNotNull();
      Assertions.assertThat(agentHeader.getValue()).isEqualTo("broumbroumserver");

      return true;
    }));
  }

}