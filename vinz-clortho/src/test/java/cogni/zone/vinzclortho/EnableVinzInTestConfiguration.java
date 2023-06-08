package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
import org.apache.http.entity.StringEntity;
import org.assertj.core.api.Assertions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Configuration
@RequiredArgsConstructor
@EnableVinzClorthoProxy
public class EnableVinzInTestConfiguration {
  @Bean
  @Profile("patch-body1")
  public BodyEditor bodyEditor() {
    return (request, originalBody) -> {
      Supplier<InputStream> inputSteamSupplier = (Supplier<InputStream>) request.getAttribute(CacheBodyFilter.bodyContentProviderAttributeKey);
      String originalContent = TestHelper.toString(inputSteamSupplier.get());
      Assertions.assertThat(originalContent).isEqualTo("Jodela");
      return new StringEntity("Phone home", StandardCharsets.UTF_8);
    };
  }
}

