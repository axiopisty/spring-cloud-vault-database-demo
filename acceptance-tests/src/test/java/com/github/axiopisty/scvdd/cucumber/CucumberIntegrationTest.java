package com.github.axiopisty.scvdd.cucumber;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
  glue = "com.github.axiopisty.scvdd.cucumber",
  features = "src/test/resources/features"
)
public class CucumberIntegrationTest {
}
