package cogni.zone.vinzclortho;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Allows modifying the response body before it is sent back to the client.
 * @since 2.0.7
 */
@FunctionalInterface
public interface ResponseEditor {

  /**
   * Edit the response body before it is sent back to the client.
   * @param data Information about the request and the response stream.
   * @return The adapted response content as a {@code java.io.InputStream} object.
   */
  @Nonnull
  InputStream editResponse(Data data) throws IOException;

  @RequiredArgsConstructor(access = AccessLevel.MODULE)
  @Getter
  class Data {
    private final HttpServletRequest originalRequest;
    private final CloseableHttpResponse httpResponse;
  }
}
