package com.github.axiopisty.scvdd.restapi.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationSecrets {

  private String someSecret;

}
