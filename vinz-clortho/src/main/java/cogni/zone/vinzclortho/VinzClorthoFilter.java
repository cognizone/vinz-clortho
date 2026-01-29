package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanExpressionContextAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;

import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static cogni.zone.vinzclortho.CacheBodyFilter.bodyContentProviderAttributeKey;
import static cogni.zone.vinzclortho.CacheBodyFilter.bodySizeProviderAttributeKey;

@RequiredArgsConstructor
@Slf4j
public class VinzClorthoFilter implements Filter {

  //TODO: put this in config this...
  //in case we config this, Content-Length should be kept out as it's set by the client doing the proxied call
  private static final String[] requestHeadersToPass = {"Accept", "Accept-Language", "Content-Type", "User-Agent"};
  private static final String[] responseHeadersToPass = {"Content-Type"};

  private final Map<String, BiFunction<String, HttpServletRequest, HttpRequestBase>> requestBuilderPerHttpMethod = init();

  private final RouteConfigurationService routeConfigurationService;
  private final HttpClientFactory httpClientFactory;
  private final Optional<RequestValidator> requestValidator;
  private final Optional<BodyEditor> bodyEditor;
  private final Optional<ResponseEditor> responseEditor;
  private final ApplicationContext context;

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
    BiFunction<String, HttpServletRequest, HttpRequestBase> requestFunction = prepareRequestFunction(httpRequest);
    if (null == requestFunction) {
      sendResponse(httpResponse, HttpResponse.of(HttpStatus.METHOD_NOT_ALLOWED));
      return;
    }

    Supplier<InputStream> attribute = (Supplier<InputStream>) httpRequest.getAttribute(bodyContentProviderAttributeKey);
    Optional<HttpResponse> httpStatus = requestValidator.map(validator -> validator.validate(new RequestValidator.Request(httpRequest, attribute)));
    if (httpStatus.isPresent()) {
      log.debug("Validator: Request not allowed - returning {}", httpStatus.get());
      sendResponse(httpResponse, httpStatus.get());
      return;
    }

    String url = constructUrl(route, httpRequest);
    HttpRequestBase request = createRequest(requestFunction, url, httpRequest, route);
    try (CloseableHttpClient client = httpClientFactory.create()) {
      log.info("Executing proxied request to {}", url);
      CloseableHttpResponse proxiedResponse = client.execute(request);
      StatusLine statusLine = proxiedResponse.getStatusLine();
      log.info("Request done: {}", statusLine);
      httpResponse.setStatus(statusLine.getStatusCode());

      passResponseHeaders(proxiedResponse, httpResponse, route.getHeaders().getResponsePass());

      route.getHeaders()
           .getResponseSet()
           .stream()
           .filter(header -> applyHeader(header, httpRequest))
           .forEach(header -> httpResponse.setHeader(header.getKey(), calculateHeaderValue(header)));

      log.debug("Proxying content...");
      IOUtils.copy(getProxiedInputStream(httpRequest, proxiedResponse), httpResponse.getOutputStream());
      log.debug("Proxying content done - request done");
    }
  }

  private InputStream getProxiedInputStream(HttpServletRequest httpRequest, CloseableHttpResponse proxiedResponse) throws IOException {
    if (responseEditor.isEmpty()) return proxiedResponse.getEntity().getContent();

    ResponseEditor.Data responseEditorData = new ResponseEditor.Data(httpRequest, proxiedResponse);
    return responseEditor.get().editResponse(responseEditorData);
  }

  private void passResponseHeaders(CloseableHttpResponse proxiedResponse, HttpServletResponse httpResponse, Collection<String> additionalHeaders) {
    Stream.concat(Arrays.stream(responseHeadersToPass), additionalHeaders.stream())
          .distinct()
          .map(proxiedResponse::getHeaders)
          .flatMap(Arrays::stream)
          .forEach(header -> httpResponse.addHeader(header.getName(), header.getValue()));
  }

  private String calculateHeaderValue(RouteConfigurationService.Header header) {
    if (header.getEvaluate() == RouteConfigurationService.EvaluateType.spel || header.getEvaluate() == RouteConfigurationService.EvaluateType.spelNoLog) {
      try {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(header.getValue(), new TemplateParserContext());

        //Add EvaluationContext to allow access to spring context in SpEL expressions
        Object patchedValue = exp.getValue(createEvaluationContext());
        if (null == patchedValue) throw new NullPointerException("Patch header value returned null");
        if (header.getEvaluate() != RouteConfigurationService.EvaluateType.spelNoLog) log.info("Patched header [{}] : from [{}] to [{}]", header.getKey(), header.getValue(), patchedValue);
        return patchedValue.toString();
      }
      catch (Exception e) {
        if (header.getEvaluate() == RouteConfigurationService.EvaluateType.spelNoLog) log.warn("Patch header key [{}] failed - returning empty: ", header.getKey(), e);
        else log.warn("Patch header key [{}] value [{}] failed - returning empty: ", header.getKey(), header.getValue(), e);
        return "";
      }
    }
    return header.getValue();
  }

  private EvaluationContext createEvaluationContext() {
    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
    evaluationContext.setBeanResolver(new BeanFactoryResolver(context));
    evaluationContext.addPropertyAccessor(new BeanExpressionContextAccessor());
    return evaluationContext;
  }

  private boolean applyHeader(RouteConfigurationService.Header header, HttpServletRequest httpRequest) {
    if (StringUtils.isBlank(header.getFilter())) return true;

    try {
      ExpressionParser parser = new SpelExpressionParser();
      Expression exp = parser.parseExpression(header.getFilter(), new TemplateParserContext());
      Boolean result = (Boolean) exp.getValue(Map.of("request", httpRequest));
      if (null == result) throw new NullPointerException("Filter returned null");
      return result;
    }
    catch (Exception e) {
      log.warn("Filter [{}] failed - keeping the header: ", header.getFilter(), e);
      return true;
    }
  }

  @Nullable
  private BiFunction<String, HttpServletRequest, HttpRequestBase> prepareRequestFunction(HttpServletRequest httpRequest) {
    String method = httpRequest.getMethod();
    return requestBuilderPerHttpMethod.get(method.toUpperCase(Locale.ROOT));
  }

  private void sendResponse(HttpServletResponse httpServletResponse, HttpResponse httpResponse) throws IOException {
    httpServletResponse.setStatus(httpResponse.getStatus());
    ServletOutputStream outputStream = httpServletResponse.getOutputStream();


    outputStream.write(httpResponse.getMessage().getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
  }

  private HttpRequestBase createRequest(BiFunction<String, HttpServletRequest, HttpRequestBase> requestFunction,
                                        String url,
                                        HttpServletRequest httpRequest,
                                        RouteConfigurationService.Route route) {
    HttpRequestBase request = requestFunction.apply(url, httpRequest);

    passRequestHeaders(httpRequest, request, route.getHeaders().getRequestPass());

    route.getHeaders()
         .getRequestSet()
         .forEach(headerToSet -> request.addHeader(headerToSet.getKey(), calculateHeaderValue(headerToSet)));

    return request;
  }

  private void passRequestHeaders(HttpServletRequest httpRequest, HttpRequestBase request, Collection<String> additionalHeaders) {
    Stream.concat(Arrays.stream(requestHeadersToPass), additionalHeaders.stream())
          .distinct()
          .forEach(headerName -> addRequestHeader(httpRequest, request, headerName));
  }

  private void addRequestHeader(HttpServletRequest httpRequest, HttpRequestBase request, String headerName) {
    Enumeration<String> headerValues = httpRequest.getHeaders(headerName);
    while (headerValues.hasMoreElements()) {
      request.addHeader(headerName, headerValues.nextElement());
    }
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
    if (routeConfigurationService.getHttpClientConfiguration().isAllowDeleteBody()) {
      HttpEntityDelete httpEntityDelete = new HttpEntityDelete(url);
      setBody(httpEntityDelete, httpRequest);
      return httpEntityDelete;
    }
    return new HttpDelete(url);
  }

  private void setBody(HttpEntityEnclosingRequest request, HttpServletRequest httpRequest) {
    @SuppressWarnings("unchecked")
    Supplier<InputStream> attribute = (Supplier<InputStream>) httpRequest.getAttribute(bodyContentProviderAttributeKey);
    HttpEntity httpEntity = bodyEditor.map(be -> be.editBody(new BodyEditor.Request(httpRequest, attribute.get()))) // map() can return null...
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
