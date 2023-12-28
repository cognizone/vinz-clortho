package cogni.zone.vinzclortho;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class HttpResponse {
  private final int status;
  private final String message;

  public static HttpResponse of(HttpStatus httpStatus) {
    return new HttpResponse(httpStatus.value(), httpStatus.getReasonPhrase());
  }
}
