package cogni.zone.vinzclortho;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.inject.Inject;

@AutoConfigureMockMvc
@ContextConfiguration(classes = EnableVinzInTestConfiguration.class)
public abstract class GoVinzTest {
  @Inject
  protected MockMvc mockMvc;

  @MockitoBean
  protected HttpClientFactory httpClientFactory;

  @BeforeEach
  public void beforeTestMethod() {
    //nada
  }


}
