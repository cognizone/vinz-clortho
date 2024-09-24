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

  private boolean matches(Route route, HttpServletRequest request) {
    return new AntPathMatcher().match(route.getPath(), request.getServletPath());
  }

  @Data
  public static class Configuration {
    private List<Route> routes = Collections.synchronizedList(new ArrayList<>());
  }

  @Data
  public static class Route {
    private String name;
    private String path;
    private String url;
    private Headers headers = new Headers();
  }

  @Data
  public static class Headers {
    private List<Header> responseSet = new ArrayList<>();
  }

  @Data
  public static class Header {
    private String key;
    private String value;
    private String filter;
  }
}
