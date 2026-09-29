package cogni.zone.vinzclortho;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
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
@ActiveProfiles({"test-basic1", "test-deleteBodyDisabled"})
class DeleteBodyDisabledTest extends GoVinzTest {
  //Explicitly disabled

  @Test
  public void runDefaultDeleteNoBody() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpClientFactory.create()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpDelete.class))).thenReturn(httpResponse);
    when(httpResponse.getCode()).thenReturn(200);
    when(httpResponse.getHeaders(any())).thenReturn(new Header[0]);
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("IT".getBytes(StandardCharsets.UTF_8)));

    MockHttpServletRequestBuilder testPostRequest = delete("/proxy/basic1/test")
            .servletPath("/proxy/basic1/test")
            .content("Jodela");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk())
           .andExpect(content().string( "IT"));

    AtomicBoolean realRequestBodyChecked = new AtomicBoolean(false);
    verify(httpClient, times(1)).execute(any());
    verify(httpClient).execute(argThat(thaRealRequest -> {
      if (realRequestBodyChecked.getAndSet(true)) return true; //called twice for some reason, just check once otherwise inputstream is gone
      Assertions.assertThat(thaRealRequest).isExactlyInstanceOf(HttpDelete.class);
      return true;
    }));
    Assertions.assertThat(realRequestBodyChecked).isTrue();
  }

}