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

import jakarta.servlet.Filter;
import java.util.Optional;

@SuppressWarnings("ClassHasNoToStringMethod")
@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
@Slf4j
public class VinzClorthoConfiguration {
  private final Optional<RequestValidator> requestValidator;
  private final Optional<BodyEditor> bodyEditor;

  @SuppressWarnings("WeakerAccess")
  public static final int cacheBodyFilterOrder = Ordered.HIGHEST_PRECEDENCE;
  @SuppressWarnings("WeakerAccess")
  public static final int mainFilterOrder = Ordered.LOWEST_PRECEDENCE;

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
    filterFilterRegistrationBean.setFilter(new VinzClorthoFilter(routeConfigurationService(), httpClientFactory(), requestValidator, bodyEditor));
    filterFilterRegistrationBean.setOrder(mainFilterOrder);
    filterFilterRegistrationBean.setName("vinzClorthoMainFilter");
    return filterFilterRegistrationBean;
  }

  @Bean
  @Lazy(false)
  public FilterRegistrationBean<Filter> vinzClorthoCacheBodyFilter() {
    FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new CacheBodyFilter(routeConfigurationService()));
    filterFilterRegistrationBean.setOrder(cacheBodyFilterOrder);
    filterFilterRegistrationBean.setName("vinzClorthoCacheBodyFilter");
    return filterFilterRegistrationBean;
  }

  @Bean
  public HttpClientFactory httpClientFactory() {
    return () -> HttpClientBuilder.create().build();
  }

}

