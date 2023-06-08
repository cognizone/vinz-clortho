package cogni.zone.vinzclortho;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientFactory {
  CloseableHttpClient create();
}
