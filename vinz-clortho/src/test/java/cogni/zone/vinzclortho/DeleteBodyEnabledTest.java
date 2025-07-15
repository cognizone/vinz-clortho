package cogni.zone.vinzclortho;

import cogni.zone.vinzclortho.http.HttpEntityDelete;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@ActiveProfiles({"test-basic1", "test-deleteBodyEnabled"})
class DeleteBodyEnabledTest extends GoVinzTest {

  @Test
  public void runDeleteWithBody() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpEntityDelete.class))).thenReturn(httpResponse);
    when(statusLine.getStatusCode()).thenReturn(200);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("IT".getBytes(StandardCharsets.UTF_8)));

    MockHttpServletRequestBuilder testPostRequest = delete("/proxy/basic1/test")
            .servletPath("/proxy/basic1/test")
            .content("Jodela");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(content().string("IT"));

    AtomicBoolean realRequestBodyChecked = new AtomicBoolean(false);
    verify(httpClient, times(1)).execute(any());
    verify(httpClient).execute(argThat(thaRealRequest -> {
      if (realRequestBodyChecked.getAndSet(true)) return true; //called twice for some reason, just check once otherwise inputstream is gone
      Assertions.assertThat(thaRealRequest).isExactlyInstanceOf(HttpEntityDelete.class);

      HttpEntity entity = ((HttpEntityEnclosingRequest) thaRealRequest).getEntity();
      String content = TestHelper.toString(entity);
      Assertions.assertThat(content).isEqualTo("Jodela");
      return true;
    }));
    Assertions.assertThat(realRequestBodyChecked).isTrue();
  }

}