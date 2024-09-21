package cogni.zone.vinzclortho;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// this class should be libized separately...
@RequiredArgsConstructor
public class CacheBodyFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(CacheBodyFilter.class);

  public static final String bodyContentProviderAttributeKey = "cogni.zone.vinzclortho.CacheBodyFilter.bodyContentProvider";
  public static final String bodySizeProviderAttributeKey = "cogni.zone.vinzclortho.CacheBodyFilter.bodySizeProvider";

  private static final byte[] emptyByteArray = new byte[0];
  private static final int maxBytesInMemory = 1024 << 10; //1MB (I just alt-entered...)

  private final RouteConfigurationService routeConfigurationService;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (routeConfigurationService.hasMatchingRoute(httpRequest)) {
      //We patch the request because vinz is nice and lets everybody pass before him
      //  But not everybody is as nice as vinz, and it might be that the body of the request is already consumed when we get to vinz (foei!)
      log.debug("Vinz matches, patching request");
      doPatchFilter(httpRequest, response, chain);
    }
    else {
      log.debug("No vinz");
      chain.doFilter(request, response);
    }
  }
  private void doPatchFilter(HttpServletRequest httpRequest, ServletResponse response, FilterChain chain) throws ServletException, IOException {
    Runnable cleanupCall = null;
    InputStream wrappedBodyInputStream;
    Supplier<InputStream> cachedBodyInputStreamProvider;
    int contentLength = httpRequest.getContentLength();
    long realContentLength;
    if (-1 == contentLength || contentLength > maxBytesInMemory) {
      List<InputStream> toClose = new ArrayList<>();
      File tempFile = File.createTempFile("vinzClortho", "bodyCache.dat");
      FileUtils.copyToFile(httpRequest.getInputStream(), tempFile);
      cleanupCall = () -> cleanup(toClose, tempFile);
      cachedBodyInputStreamProvider = () -> getInputStreamForFile(toClose, tempFile);
      wrappedBodyInputStream = getInputStreamForFile(toClose, tempFile);
      realContentLength = tempFile.length();
    }
    else {
      byte[] bytes = 0 == contentLength ? emptyByteArray : IOUtils.toByteArray(httpRequest.getInputStream());
      cachedBodyInputStreamProvider = () -> new ByteArrayInputStream(bytes);
      wrappedBodyInputStream = new ByteArrayInputStream(bytes);
      realContentLength = bytes.length;
    }

    httpRequest.setAttribute(bodyContentProviderAttributeKey, cachedBodyInputStreamProvider);
    httpRequest.setAttribute(bodySizeProviderAttributeKey, realContentLength);
    HttpServletRequest wrapper = new VinzServletRequestWrapper(httpRequest, new BodyInputStream(wrappedBodyInputStream));

    try {
      chain.doFilter(wrapper, response);
    }
    finally {
      if (null != cleanupCall) cleanupCall.run();
    }
  }

  private InputStream getInputStreamForFile(List<InputStream> toClose, File tempFile) {
    try {
      InputStream inputStream = new FileInputStream(tempFile);
      toClose.add(inputStream);
      return inputStream;
    }
    catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void cleanup(List<InputStream> inputStreams, File file) {
    inputStreams.forEach(IOUtils::closeQuietly);
    if (!file.delete()) file.deleteOnExit();
  }

  private static class VinzServletRequestWrapper extends HttpServletRequestWrapper {
    private final BodyInputStream bodyInputStream;

    private VinzServletRequestWrapper(HttpServletRequest request, BodyInputStream bodyInputStream) {
      super(request);
      this.bodyInputStream = bodyInputStream;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return bodyInputStream;
    }
  }

  private static class BodyInputStream extends ServletInputStream {

    private final InputStream inputStream;

    private BodyInputStream(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
      throw new UnsupportedOperationException("Nop");
    }

    @Override
    public int read() throws IOException {
      return inputStream.read();
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
      return inputStream.read(bytes, off, len);
    }

    @Override
    public int read(byte[] bytes) throws IOException {
      return inputStream.read(bytes);
    }

    @Override
    public long skip(long byteCount) throws IOException {
      return inputStream.skip(byteCount);
    }

    @Override
    public int available() throws IOException {
      return inputStream.available();
    }

    @Override
    public void close() throws IOException {
      inputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
      inputStream.reset();
    }

    @Override
    public boolean markSupported() {
      return inputStream.markSupported();
    }
  }


}