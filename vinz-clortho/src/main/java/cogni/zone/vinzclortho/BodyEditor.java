package cogni.zone.vinzclortho;

import org.apache.http.HttpEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;

@FunctionalInterface
public interface BodyEditor {
  HttpEntity editBody(HttpServletRequest request, InputStream originalBody);
}
