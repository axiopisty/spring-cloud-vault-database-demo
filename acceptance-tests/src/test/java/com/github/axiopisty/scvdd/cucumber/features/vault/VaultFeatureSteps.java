package com.github.axiopisty.scvdd.cucumber.features.vault;

import org.apache.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class VaultFeatureSteps {

  private static Duration timeout = Duration.ofSeconds(5);

  private ClientResponse clientResponse;

  public void httpGet(String path) {

    final String port = System.getProperty("rest-api.port");

    clientResponse = WebClient
      .builder()
      .baseUrl("http://localhost:" + port)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build()
      .method(HttpMethod.GET)
      .uri(path)
      .exchange()
      .block(timeout);
  }

  public void validateResponseStatusCodeIs(int statusCode) {
    assertThat(clientResponse.statusCode().value()).isEqualTo(statusCode);
  }

  public void validateResponseBodyIs(String json) {
    String actual = clientResponse.bodyToMono(String.class).block(timeout);
    assertThat(actual).isEqualTo(json);
  }

}
