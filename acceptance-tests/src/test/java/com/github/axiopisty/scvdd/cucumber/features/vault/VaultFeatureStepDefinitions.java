package com.github.axiopisty.scvdd.cucumber.features.vault;

import com.fasterxml.jackson.core.JsonProcessingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.thucydides.core.annotations.Steps;

public class VaultFeatureStepDefinitions {

  @Steps
  private VaultFeatureSteps steps;

  @When("^an http GET request is issued to (.+)$")
  public void whenAnHttpGetRequestIsIssuedTo(String path) {
    steps.httpGet(path);
  }

  @Then("^the response status should be (\\d+)$")
  public void thenTheResponseStatusShouldBe(int statusCode) {
    steps.validateResponseStatusCodeIs(statusCode);
  }

  @And("^the response body should be (.+)$")
  public void andTheResponseBodyShouldBe(String json) throws JsonProcessingException {
    steps.validateResponseBodyIs(json);
  }
}
