package cogni.zone.vinzclortho.http;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpEntityDelete extends HttpEntityEnclosingRequestBase {
  public HttpEntityDelete(String uri) {
    setURI(URI.create(uri));
  }

  @Override
  public String getMethod() {
    return HttpDelete.METHOD_NAME;
  }
}
