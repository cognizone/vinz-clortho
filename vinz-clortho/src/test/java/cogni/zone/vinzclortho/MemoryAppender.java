package cogni.zone.vinzclortho;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.Arrays;

class MemoryAppender extends ListAppender<ILoggingEvent> {
  public void reset() {
    list.clear();
  }

  public boolean hasContainsAll(String... values) {
    if (values.length == 0) throw new IllegalArgumentException("At least one value must be provided");
    return list.stream()
               .map(ILoggingEvent::toString)
               .anyMatch(event -> containsAll(event, values));
  }

  public long countContainsAll(String... values) {
    if (values.length == 0) throw new IllegalArgumentException("At least one value must be provided");
    return list.stream()
               .map(ILoggingEvent::toString)
               .filter(event -> containsAll(event, values))
               .count();

  }

  private boolean containsAll(String logString, String... values) {
    return Arrays.stream(values)
                 .allMatch(logString::contains);
  }

}