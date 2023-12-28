package cogni.zone.vinzclortho;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@ActiveProfiles({"test-basic1", "requestValidator-failOnBlup"})
class RequestValidator1Test extends GoVinzTest {

  @Test
  void allFine() throws Exception {
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


    MockHttpServletRequestBuilder testPostRequest = post("/proxy/basic1/test")
            .servletPath("/proxy/basic1/test")
            .content("Jodela");
    mockMvc.perform(testPostRequest)
           .andExpect(status().isOk());


    verify(httpClient, times(1)).execute(any());
  }

  @Test
  void kaboum_content() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    when(httpClientFactory.create()).thenReturn(httpClient);

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/basic1/test")
            .servletPath("/proxy/basic1/test")
            .content("TheContentContainsBlupAndSomeMoreStuff");
    mockMvc.perform(testPostRequest)
           .andExpect(status().is(400))
           .andExpect(content().string("contentWithBlup"));

    verify(httpClient, times(0)).execute(any());
  }

  @Test
  void kaboum_param() throws Exception {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    when(httpClientFactory.create()).thenReturn(httpClient);

    MockHttpServletRequestBuilder testPostRequest = post("/proxy/basic1/test")
            .servletPath("/proxy/basic1/test")
            .param("blup", "wizzzzzz")
            .content("Jodela");
    mockMvc.perform(testPostRequest)
           .andExpect(status().is(400))
           .andExpect(content().string("param: wizzzzzz"));

    verify(httpClient, times(0)).execute(any());
  }

}