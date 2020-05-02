package com.github.axiopisty.scvdd.restapi.internal;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.Lease;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.vault.support.VaultResponseSupport;

import java.util.function.Function;

import static java.util.Optional.ofNullable;

@SuppressWarnings("PointlessBooleanExpression")
@Configuration
@Log4j2
public class VaultConfiguration {

  public VaultConfiguration(
    SecretLeaseContainer leaseContainer
  ) {
    leaseContainer.addLeaseListener(this::printEventDetails);
  }

  @Bean
  public ApplicationSecrets applicationSecrets(VaultTemplate vaultTemplate) {
    final String VAULT_SECRETS_PATH = stripTrailingSlash(System.getenv("VAULT_SECRETS_PATH"));
    final Function<String, String> secretLocator = path -> {
      try {
        final String vaultPath = VAULT_SECRETS_PATH + "/" + path;
        return
          ofNullable(vaultTemplate.read(vaultPath))
          .map(VaultResponseSupport::getData)
          .map(x -> x.get("value"))
          .map(Object::toString)
          .orElseThrow();
      } catch (Exception e) {
        throw new BeanInitializationException("Unable to read vault secret at path: " + path, e);
      }
    };

    return ApplicationSecrets
      .builder()
      .someSecret(secretLocator.apply("some-secret"))
      .build();
  }

  private void printEventDetails(SecretLeaseEvent event) {
    final String name = event.getClass().getSimpleName();
    final RequestedSecret secret = event.getSource();
    final Lease lease = event.getLease();
    final long timestamp = event.getTimestamp();
    log.info("SecretLeaseEvent [type={}, timestamp={}] {} {}", name, timestamp, lease, secret);
  }

  private String stripTrailingSlash(String input) {
    final String output;
    if(StringUtils.isEmpty(input) == false && input.endsWith("/")) {
      output = input.substring(0, input.length() - 1);
    } else {
      output = input;
    }
    return output;
  }

}
