package it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hascode.tutorial.UpstreamService;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LatencyTest {
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(9999);

  ToxiproxyClient client;
  Proxy httpProxy;

  @Before
  public void setup() throws Exception {
    client = new ToxiproxyClient("127.0.0.1", 8474);
    httpProxy = client.createProxy("http-tproxy", "127.0.0.1:8888", "127.0.0.1:9999");
    httpProxy.toxics().latency("latency-toxic", ToxicDirection.DOWNSTREAM, 12_000).setJitter(15);
  }

  @After
  public void teardown() throws Exception {
    httpProxy.delete();
  }

  @Test
  public void latencyTest() {
    stubFor(get(urlEqualTo("/rs/date"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(String.format("{\"now\":\"%s\"}", LocalDateTime.now()))));

    UpstreamService upstreamService = new UpstreamService();
    upstreamService.callRestEndpoint("http://localhost:8888/rs/date");


    verify(getRequestedFor(urlMatching("/rs/date"))
        .withHeader("Accept", matching("application/json")));
  }


}
