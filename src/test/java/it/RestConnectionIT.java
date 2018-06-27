package it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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

public class RestConnectionIT {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(9999);

  ToxiproxyClient client;
  Proxy httpProxy;

  @Before
  public void setup() throws Exception {
    client = new ToxiproxyClient("127.0.0.1", 8474);
    httpProxy = client.createProxy("http-tproxy", "127.0.0.1:8888", "127.0.0.1:9999");
  }

  @After
  public void teardown() throws Exception {
    httpProxy.delete();
  }

  @Test
  public void latencyTest() throws Exception {
    // create toxic
    httpProxy.toxics().latency("latency-toxic", ToxicDirection.DOWNSTREAM, 12_000).setJitter(15);

    // create fake rest endpoint
    stubFor(get(urlEqualTo("/rs/date"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(String.format("{\"now\":\"%s\"}", LocalDateTime.now()))));

    // call rest service over toxiproxy
    UpstreamService upstreamService = new UpstreamService();
    upstreamService.callRestEndpoint("http://localhost:8888/rs/date");

    // verify something happened
    verify(getRequestedFor(urlMatching("/rs/date"))
        .withHeader("Accept", matching("application/json")));
  }

  @Test
  public void bandWidthTest() throws Exception {
    // create toxic with 1.5Mbit bandwidth limit
    httpProxy.toxics().bandwidth("bandwidth-toxic", ToxicDirection.UPSTREAM, 150);

    // create fake rest endpoint
    stubFor(post(urlEqualTo("/rs/data/upload"))
        .withHeader("Content-type", equalTo("application/octet-stream"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text-plain")
            .withBody("data received")));

    // call rest service over toxiproxy
    UpstreamService upstreamService = new UpstreamService();
    upstreamService.sendToRestEndpoint("http://localhost:8888/rs/data/upload", 1_048_576);

    // verify something happened
    verify(postRequestedFor(urlMatching("/rs/data/upload"))
        .withHeader("Content-type", matching("application/octet-stream")));
  }


}
