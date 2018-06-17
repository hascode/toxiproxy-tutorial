package com.hascode.tutorial;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpClient.Version;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

public class App {

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException {
    ToxiproxyClient client = new ToxiproxyClient("127.0.0.1", 8474);
    Proxy httpProxy = client.createProxy("http-tproxy", "127.0.0.1:8888", "app.hascode.com:80");
    httpProxy.toxics().latency("latency-toxic", ToxicDirection.DOWNSTREAM, 4000).setJitter(15);
    HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.1:8888/forensic/"))
        .header("Host", "app.hascode.com")
        .GET().build();
    HttpClient
        .newBuilder()
        .version(Version.HTTP_1_1)
        .build()
        .sendAsync(request, HttpResponse.BodyHandler.asString()).thenApply(HttpResponse::body)
        .thenAccept(System.out::println).join();
  }
}
