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
  public static final int filterOrder = Ordered.LOWEST_PRECEDENCE;

  @Bean
  @ConfigurationProperties(prefix = "cognizone.vinz")
  public VinzClorthoFilter.Configuration vinzClorthoConfig() {
    return new VinzClorthoFilter.Configuration();
  }

  @Bean
  @Lazy(false)
  public FilterRegistrationBean<Filter> vinzClorthoFilter() {
    FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new VinzClorthoFilter(vinzClorthoConfig()));
    filterFilterRegistrationBean.setOrder(filterOrder);
    filterFilterRegistrationBean.setName("vinzClorthoFilter");
    return filterFilterRegistrationBean;
  }

}

