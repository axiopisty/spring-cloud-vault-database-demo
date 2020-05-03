package com.github.axiopisty.scvdd.restapi.internal.api;

import com.github.axiopisty.scvdd.restapi.internal.ApplicationSecrets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecretsController {

  private final ApplicationSecrets secrets;

  public SecretsController(ApplicationSecrets secrets) {
    this.secrets = secrets;
  }

  /**
   * You wouldn't really create a method that exposes your application
   * secrets, but for our purposes, since this is just a demo application,
   * that's exactly what we are going to do. We'll build a test case around
   * it for the demonstration.
   *
   * @return application secrets - don't really do this in production code
   */
  @GetMapping("/api/secrets")
  public ResponseEntity<ApplicationSecrets> getApplicationSecrets() {
    return ResponseEntity.ok(secrets);
  }

}
