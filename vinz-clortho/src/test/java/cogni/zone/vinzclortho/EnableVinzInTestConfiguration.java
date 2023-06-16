package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
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
}

