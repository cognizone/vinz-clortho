package cogni.zone.vinzclortho;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  public static final String defaultResponseBase = "InsideTestController.";

  public static final String pathBase = "/proxy/basic2/testController";
  public static final String pathWithSubPath = "/proxy/basic2/testController/with/extra/path";

  @GetMapping(path = pathBase)
  public ResponseEntity<String> testController1() {
    return ResponseEntity.ok(defaultResponseBase + pathBase);
  }

  @GetMapping(path = pathWithSubPath)
  public ResponseEntity<String> testController2() {
    return ResponseEntity.ok(defaultResponseBase + pathWithSubPath);
  }

}
