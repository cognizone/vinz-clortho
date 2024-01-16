package cogni.zone.vinzclortho;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.function.Supplier;

@FunctionalInterface
public interface RequestValidator {

  /**
   * @param request Information about the request.
   * @return The status to return (proxying will not happen) or null to continue proxying the request.
   */
  @Nullable
  HttpResponse validate(@Nonnull Request request);

  @RequiredArgsConstructor(access = AccessLevel.MODULE)
  @Getter
  class Request {
    private final HttpServletRequest request;
    private final Supplier<InputStream> bodySupplier;
  }

}
