package cogni.zone.vinzclortho;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.inject.Inject;

@AutoConfigureMockMvc
@ContextConfiguration(classes = EnableVinzInTestConfiguration.class)
@MockBeans(@MockBean(HttpClientFactory.class))
public abstract class GoVinzTest {
  @Inject
  protected MockMvc mockMvc;

  @Inject
  protected HttpClientFactory httpClientFactory;

  @BeforeEach
  public void beforeTestMethod() {
    //nada
  }


}
