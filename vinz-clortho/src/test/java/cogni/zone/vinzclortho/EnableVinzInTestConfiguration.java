package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.StringEntity;
import org.assertj.core.api.Assertions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
@EnableVinzClorthoProxy
public class EnableVinzInTestConfiguration {
  @Bean
  @Profile("patch-body1")
  public BodyEditor bodyEditor1() {
    return requestInfo -> {
      String originalContent = TestHelper.toString(requestInfo.getOriginalBody());
      Assertions.assertThat(originalContent).isEqualTo("Jodela");
      return new StringEntity("Phone home", StandardCharsets.UTF_8);
    };
  }

  @Bean
  @Profile("patch-body2")
  public BodyEditor bodyEditor2() {
    return requestInfo -> {
      String originalContent = TestHelper.toString(requestInfo.getOriginalBody());
      Assertions.assertThat(originalContent).isEqualTo("Jodela");
      return null;
    };
  }

  @Bean
  @Profile("requestValidator-failOnBlup")
  public RequestValidator requestValidator1() {
    return requestInfo -> {
      String originalContent = TestHelper.toString(requestInfo.getBodySupplier().get());
      if (originalContent.contains("Blup")) return new HttpResponse(400, "contentWithBlup");

      String blup = requestInfo.getRequest().getParameter("blup");
      if (StringUtils.isNotBlank(blup)) return new HttpResponse(400, "param: " + blup);

      return null;
    };
  }

  @Bean
  @Profile("patch-response1")
  public ResponseEditor responseEditor1() {
    return data -> {
      String value = IOUtils.toString(data.getHttpResponse().getEntity().getContent(), StandardCharsets.UTF_8);
      String patchedValue = "[[" + value + "]]";
      return IOUtils.toInputStream(patchedValue, StandardCharsets.UTF_8);
    };
  }

}

