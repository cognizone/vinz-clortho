package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static cogni.zone.vinzclortho.CacheBodyFilter.bodyContentProviderAttributeKey;
import static cogni.zone.vinzclortho.CacheBodyFilter.bodySizeProviderAttributeKey;

@RequiredArgsConstructor
public class VinzClorthoFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(VinzClorthoFilter.class);

  //TODO: put this in config this...
  //in case we config this, Content-Length should be kept out as it's set by the client doing the proxied call
  private static final String[] requestHeadersToPass = {"Accept", "Accept-Language", "Content-Type", "User-Agent"};
  private static final String[] responseHeadersToPass = {"Content-Type"};

  private final Map<String, BiFunction<String, HttpServletRequest, HttpRequestBase>> requestBuilderPerHttpMethod = init();

  private final RouteConfigurationService routeConfigurationService;
  private final HttpClientFactory httpClientFactory;
  private final Optional<BodyEditor> bodyEditor;

  private Map<String, BiFunction<String, HttpServletRequest, HttpRequestBase>> init() {
    return Collections.synchronizedMap(Map.of("GET", this::prepareGetRequest,
                                              "POST", this::preparePostRequest,
                                              "PUT", this::preparePutRequest,
                                              "DELETE", this::prepareDeleteRequest));
  }

  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    Optional<RouteConfigurationService.Route> matchingRoute = routeConfigurationService.findRoute(httpRequest);
    if (matchingRoute.isEmpty()) {
      chain.doFilter(request, response);
    }
    else {
      routeThis(httpRequest, (HttpServletResponse) response, matchingRoute.get());
    }
  }


  private void routeThis(HttpServletRequest httpRequest, HttpServletResponse httpResponse, RouteConfigurationService.Route route) throws IOException {
    log.debug("Proxying {} to route {}", httpRequest.getServletPath(), route);
    String url = constructUrl(route, httpRequest);

    HttpRequestBase request = createRequest(url, httpRequest);
    try (CloseableHttpClient client = httpClientFactory.create()) {
      CloseableHttpResponse proxiedResponse = client.execute(request);
      StatusLine statusLine = proxiedResponse.getStatusLine();
      httpResponse.setStatus(statusLine.getStatusCode());

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
  private HttpRequestBase prepareGetRequest(String url, HttpServletRequest httpRequest) {
    return new HttpGet(url);
  }

  private HttpRequestBase preparePostRequest(String url, HttpServletRequest httpRequest) {
    HttpPost httpPost = new HttpPost(url);
    setBody(httpPost, httpRequest);
    return httpPost;
  }

  private HttpRequestBase preparePutRequest(String url, HttpServletRequest httpRequest) {
    HttpPut httpPut = new HttpPut(url);
    setBody(httpPut, httpRequest);
    return httpPut;
  }

  @SuppressWarnings("unused")
  private HttpRequestBase prepareDeleteRequest(String url, HttpServletRequest httpRequest) {
    return new HttpDelete(url);
  }

  private void setBody(HttpEntityEnclosingRequest request, HttpServletRequest httpRequest) {
    @SuppressWarnings("unchecked")
    Supplier<InputStream> attribute = (Supplier<InputStream>) httpRequest.getAttribute(bodyContentProviderAttributeKey);
    HttpEntity httpEntity = bodyEditor.map(be -> be.editBody(httpRequest, attribute.get()))
                                      .orElse(new InputStreamEntity(attribute.get(), (Long) httpRequest.getAttribute(bodySizeProviderAttributeKey)));
    request.setEntity(httpEntity);
  }

  private String constructUrl(RouteConfigurationService.Route route, HttpServletRequest httpRequest) {
    String servletPath = httpRequest.getServletPath();
    String extractPath = new AntPathMatcher().extractPathWithinPattern(route.getPath(), servletPath);

    String url = route.getUrl();
    if (StringUtils.isNotBlank(extractPath)) {
      boolean alreadyHas = url.endsWith("/");
      boolean hasExtra = extractPath.startsWith("/");
      if (alreadyHas && hasExtra) url += extractPath.substring(1);
      else if (alreadyHas || hasExtra) url += extractPath;
      else url += "/" + extractPath;
    }

    String queryString = httpRequest.getQueryString();
    if (null != queryString) {
      url += "?" + queryString;
    }

    log.debug("Constructed new URL: {}", url);
    return url;
  }
}
