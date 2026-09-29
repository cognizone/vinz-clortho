package cogni.zone.vinzclortho;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

public interface HttpClientFactory {
  CloseableHttpClient create();
}
