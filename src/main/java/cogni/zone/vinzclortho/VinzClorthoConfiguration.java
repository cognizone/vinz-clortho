package cogni.zone.vinzclortho;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;

import javax.servlet.Filter;

@Configuration
@EnableConfigurationProperties
public class VinzClorthoConfiguration {

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
    FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new VinzClorthoFilter(routeConfigurationService()));
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

}

