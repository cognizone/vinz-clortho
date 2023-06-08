package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;

import javax.servlet.Filter;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
@Slf4j
public class VinzClorthoConfiguration {
  private final Optional<BodyEditor> bodyEditor;

  @Bean
  @ConfigurationProperties(prefix = "cognizone.vinz")
  public RouteConfigurationService.Configuration vinzClorthoConfig() {
    return new RouteConfigurationService.Configuration();
  }

  @Bean
  public RouteConfigurationService routeConfigurationService() {
    return new RouteConfigurationService(vinzClorthoConfig());
  }

  @Bean
  @Lazy(false)
  public FilterRegistrationBean<Filter> vinzClorthoMainFilter() {
    log.info("Init filter with bodyEditor: {}", bodyEditor.isPresent());
    FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new VinzClorthoFilter(routeConfigurationService(), httpClientFactory(), bodyEditor));
    filterFilterRegistrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);
    filterFilterRegistrationBean.setName("vinzClorthoMainFilter");
    return filterFilterRegistrationBean;
  }

  @Bean
  @Lazy(false)
  public FilterRegistrationBean<Filter> vinzClorthoCacheBodyFilter() {
    FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new CacheBodyFilter(routeConfigurationService()));
    filterFilterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    filterFilterRegistrationBean.setName("vinzClorthoCacheBodyFilter");
    return filterFilterRegistrationBean;
  }

  @Bean
  public HttpClientFactory httpClientFactory() {
    return () -> HttpClientBuilder.create().build();
  }

}

