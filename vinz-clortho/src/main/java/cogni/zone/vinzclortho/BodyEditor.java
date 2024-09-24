package cogni.zone.vinzclortho;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;

@FunctionalInterface
public interface BodyEditor {
  /**
   *
   * @param request Information about the request.
   * @return The adapted body content as a {@code org.apache.http.HttpEntity} object. Can be null to use the original body.
   */
  @Nullable
  HttpEntity editBody(@Nonnull Request request);

  @RequiredArgsConstructor(access = AccessLevel.MODULE)
  @Getter
  class Request {
    private final HttpServletRequest request;
    private final InputStream originalBody;
  }
}
