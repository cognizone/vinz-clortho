package cogni.zone.vinzclortho;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestHelper {

  @SneakyThrows
  public static String toString(HttpEntity entity) {
    return toString(entity.getContent());
  }

  @SneakyThrows
  public static String toString(InputStream inputStream) {
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }
}
