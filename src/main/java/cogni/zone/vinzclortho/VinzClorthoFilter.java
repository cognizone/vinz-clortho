package cogni.zone.vinzclortho;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class VinzClorthoFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(VinzClorthoFilter.class);

  //TODO: put this in config this...
  //in case we config this, Content-Length should be kept out as it's set by the client doing the proxied call
  private static final String[] requestHeadersToPass = {"Accept", "Accept-Language", "Content-Type", "User-Agent"};
  private static final String[] responseHeadersToPass = {"Content-Type"};

  private static final Map<String, BiFunction<String, HttpServletRequest, HttpRequestBase>> requestBuilderPerHttpMethod =
          Collections.synchronizedMap(Map.of("GET", VinzClorthoFilter::prepareGetRequest,
                                             "POST", VinzClorthoFilter::preparePostRequest,
                                             "PUT", VinzClorthoFilter::preparePutRequest,
                                             "DELETE", VinzClorthoFilter::prepareDeleteRequest));

  private final Configuration config;

  public VinzClorthoFilter(Configuration config) {
    this.config = config;
    log.info("Construct with config: {}", config);
  }

  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    Optional<Configuration.Route> matchingRoute = config.getRoutes().stream()
                                                        .filter(route -> matches(route, httpRequest))
                                                        .findFirst();

    if (matchingRoute.isEmpty()) {
      chain.doFilter(request, response);
    }
    else {
      routeThis(httpRequest, (HttpServletResponse) response, matchingRoute.get());
    }

  }

  private void routeThis(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Configuration.Route route) throws IOException {
    log.debug("Proxying {} to route {}", httpRequest.getServletPath(), route);
    String url = constructUrl(route, httpRequest);

    HttpRequestBase request = createRequest(url, httpRequest);
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      CloseableHttpResponse proxiedResponse = client.execute(request);
      httpResponse.setStatus(proxiedResponse.getStatusLine().getStatusCode());

      Arrays.stream(responseHeadersToPass)
            .map(proxiedResponse::getHeaders)
            .flatMap(Arrays::stream)
            .forEach(header -> httpResponse.addHeader(header.getName(), header.getValue()));

      IOUtils.copy(proxiedResponse.getEntity().getContent(), httpResponse.getOutputStream());
    }
  }

  @SuppressWarnings("MethodWithMultipleLoops")
  private HttpRequestBase createRequest(String url, HttpServletRequest httpRequest) {
    String method = httpRequest.getMethod();
    BiFunction<String, HttpServletRequest, HttpRequestBase> requestFunction = requestBuilderPerHttpMethod.get(method.toUpperCase(Locale.ROOT));
    if (null == requestFunction) throw new RuntimeException("Vinz doesn't support " + method + " request yet.");

    HttpRequestBase request = requestFunction.apply(url, httpRequest);
    for (String headersToPass : requestHeadersToPass) {
      Enumeration<String> headerValues = httpRequest.getHeaders(headersToPass);
      while (headerValues.hasMoreElements()) {
        String headerValue = headerValues.nextElement();
        request.addHeader(headersToPass, headerValue);
      }
    }
    return request;
  }

  @SuppressWarnings("unused")
  private static HttpRequestBase prepareGetRequest(String url, HttpServletRequest httpRequest) {
    return new HttpGet(url);
  }

  private static HttpRequestBase preparePostRequest(String url, HttpServletRequest httpRequest) {
    HttpPost httpPost = new HttpPost(url);
    try {
      httpPost.setEntity(new InputStreamEntity(httpRequest.getInputStream()));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return httpPost;
  }

  private static HttpRequestBase preparePutRequest(String url, HttpServletRequest httpRequest) {
    HttpPut httpPut = new HttpPut(url);
    try {
      httpPut.setEntity(new InputStreamEntity(httpRequest.getInputStream()));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return httpPut;
  }

  @SuppressWarnings("unused")
  private static HttpRequestBase prepareDeleteRequest(String url, HttpServletRequest httpRequest) {
    return new HttpDelete(url);
  }

  private String constructUrl(Configuration.Route route, HttpServletRequest httpRequest) {
    String servletPath = httpRequest.getServletPath();
    String extractPath = new AntPathMatcher().extractPathWithinPattern(route.getPath(), servletPath);

    String url = route.getUrl();
    if (StringUtils.isNotBlank(extractPath)) {
      boolean alreadyHave = url.endsWith("/");
      boolean hasExtra = extractPath.startsWith("/");
      if (alreadyHave && hasExtra) url += extractPath.substring(1);
      else if (alreadyHave || hasExtra) url += extractPath;
      else url += "/" + extractPath;
    }

    String queryString = httpRequest.getQueryString();
    if (null != queryString) {
      url += "?" + queryString;
    }

    log.debug("Constructed new URL: {}", url);
    return url;
  }

  private boolean matches(Configuration.Route route, HttpServletRequest request) {
    return new AntPathMatcher().match(route.getPath(), request.getServletPath());
  }

  public static class Configuration {

    private List<Route> routes = new ArrayList<>();

    public List<Route> getRoutes() {
      return routes;
    }

    public void setRoutes(List<Route> routes) {
      this.routes = routes;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
              .append("routes", routes)
              .toString();
    }

    public static class Route {
      private String name;
      private String path;
      private String url;

      public String getName() {
        return name;
      }

      public Route setName(String name) {
        this.name = name;
        return this;
      }

      public String getPath() {
        return path;
      }

      public void setPath(String path) {
        this.path = path;
      }

      public String getUrl() {
        return url;
      }

      public void setUrl(String url) {
        this.url = url;
      }

      @Override
      public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("path", path)
                .append("url", url)
                .toString();
      }
    }
  }
}
