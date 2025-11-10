package cogni.zone.vinzclortho;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class RouteConfigurationService {

  private final Configuration config;

  public boolean hasMatchingRoute(HttpServletRequest httpRequest) {
    return findRoute(httpRequest).isPresent();
  }

  public Optional<Route> findRoute(HttpServletRequest httpRequest) {
    return config.getRoutes().stream()
                 .filter(route -> matches(route, httpRequest))
                 .findFirst();
  }

  public HttpClient getHttpClientConfiguration() {
    return config.getHttpClient();
  }

  private boolean matches(Route route, HttpServletRequest request) {
    String servletPath = request.getServletPath();

    if (!matches(route.getPath(), servletPath)) {
      log.debug("Route {} (path: {}) did not match for servletPath {}", route.getName(), route.getPath(), servletPath);
      return false;
    }

    log.debug("Route {} (path: {}) matched for servletPath {} - checking exclusions", route.getName(), route.getPath(), servletPath);

    for (String excludePath : route.getExcludePaths()) {
      if (!matches(excludePath, servletPath)) continue;
      log.debug("Route {} (path: {}) excluded for servletPath {}", route.getName(), excludePath, servletPath);
      return false;
    }

    log.debug("Route {} (path: {}) not excluded for servletPath {}", route.getName(), route.getPath(), servletPath);
    return true;
  }

  private boolean matches(String routePath, String servletPath) {
    return new AntPathMatcher().match(routePath, servletPath);
  }

  @Data
  public static class Configuration {
    private List<Route> routes = Collections.synchronizedList(new ArrayList<>());
    private HttpClient httpClient = new HttpClient();
  }

  @Data
  public static class HttpClient {
    private boolean useSystemProperties;
    private boolean allowDeleteBody;
  }

  @Data
  public static class Route {
    private String name;
    private String path;
    private String url;
    private List<String> excludePaths = new ArrayList<>();
    private Headers headers = new Headers();
  }

  @Data
  public static class Headers {
    private List<Header> responseSet = new ArrayList<>();
    private List<Header> requestSet = new ArrayList<>();
  }

  @Data
  public static class Header {
    private String key;
    private String value;
    private String filter;
    private EvaluateType evaluate;
  }

  public enum EvaluateType {
    /**
     * just use the value without any transformations (default value)
     */
    raw,
    /**
     * Handle the value as a SpEL expression
     */
    spel,
    /**
     * Same as `spel` but no values will be logged, useful if header contains sensitive information
     *
     * @since 2.0.5
     */
    spelNoLog
  }
}
