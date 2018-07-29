package com.hascode.tutorial;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpClient.Version;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpRequest.BodyProcessor;
import jdk.incubator.http.HttpResponse;

public class UpstreamService {

  public void callRestEndpoint(String url) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Accept", "application/json")
        .GET().build();
    Instant start = Instant.now();
    HttpClient
        .newBuilder()
        .version(Version.HTTP_1_1)
        .build()
        .sendAsync(request, HttpResponse.BodyHandler.asString()).thenApply(HttpResponse::body)
        .thenAccept(System.out::println).join();
    long durationInSeconds = Duration.between(start, Instant.now()).getSeconds();
    System.out.printf("the request took %d seconds%n", durationInSeconds);
  }

  public void sendToRestEndpoint(String url, int size) {
    byte[] body = new byte[size];

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-type", " application/octet-stream")
        .POST(BodyProcessor.fromByteArray(body)).build();
    Instant start = Instant.now();
    HttpClient
        .newBuilder()
        .version(Version.HTTP_1_1)
        .build()
        .sendAsync(request, HttpResponse.BodyHandler.asString()).thenApply(HttpResponse::body)
        .thenAccept(System.out::println).join();
    long durationInSeconds = Duration.between(start, Instant.now()).getSeconds();
    System.out.printf("uploading %d kb took %d seconds%n", (size/1024), durationInSeconds);
  }
}
