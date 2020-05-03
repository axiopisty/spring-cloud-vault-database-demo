package com.github.axiopisty.scvdd.restapi.internal;

import java.net.URI;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.authentication.CachingVaultTokenSupplier;
import org.springframework.vault.authentication.LifecycleAwareSessionManagerSupport;
import org.springframework.vault.authentication.ReactiveLifecycleAwareSessionManager;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.vault.client.ClientHttpConnectorFactory.create;
import static org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration.of;
import static org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration.unconfigured;

/**
 * Custom bootstrap configuration class used for overriding the default value of 5 REFRESH_PERIOD_BEFORE_EXPIRY
 * which is used when renewing tokens.
 */
@Log4j2
public class VaultBootstrapConfiguration {
  
  /**
   * Existing bean of ReactiveLifecycleAwareSessionManager doesn't take in a refreshTrigger and is using the default
   * trigger with the REFRESH_PERIOD_BEFORE_EXPIRY set as 5. We are initializing the class with another constructor
   * which takes in a refresh trigger object
   *
   * @param beanFactory             the bean factory
   * @param threadPoolTaskScheduler the thread pool task scheduler
   * @param vaultProperties         the vault properties
   *
   * @return the reactive session manager
   */
  @Primary
  @Bean
  public ReactiveSessionManager reactiveVaultSessionManager(
    final BeanFactory beanFactory,
    final ThreadPoolTaskScheduler threadPoolTaskScheduler,
    final VaultProperties vaultProperties
  ) {
    log.info("Setting custom ReactiveSessionManager bean");
    final VaultTokenSupplier vaultTokenSupplier = beanFactory.getBean("vaultTokenSupplier", VaultTokenSupplier.class);
    if (vaultProperties.getConfig().getLifecycle().isEnabled()) {
      final WebClient webClient = WebClientBuilder
        .builder()
        .httpConnector(createConnector(vaultProperties))
        .endpointProvider(SimpleVaultEndpointProvider.of(createVaultEndpoint(vaultProperties)))
        .build();
      return new ReactiveLifecycleAwareSessionManager(
        vaultTokenSupplier,
        threadPoolTaskScheduler,
        webClient,
        new LifecycleAwareSessionManagerSupport.FixedTimeoutRefreshTrigger(60, SECONDS)
      );
    } else {
      return CachingVaultTokenSupplier.of(vaultTokenSupplier);
    }
  }
  
  /**
   * Thread pool scheduler, this bean is created as it is required to initialize ReactiveLifecycleAwareSessionManager.
   * The one instantiated through spring cloud vault is wrapped in a wrapper class, which have a get method with scope
   * of package-private
   *
   * @param applicationContext the application context
   *
   * @return the thread pool task scheduler
   */
  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler(final ConfigurableApplicationContext applicationContext) {
    final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(2);
    threadPoolTaskScheduler.setDaemon(true);
    threadPoolTaskScheduler.setThreadNamePrefix("Spring-Cloud-Vault-");
    applicationContext.registerShutdownHook();
    return threadPoolTaskScheduler;
  }
  
  private static ClientHttpConnector createConnector(final VaultProperties vaultProperties) {
    final ClientOptions clientOptions = new ClientOptions(
      ofMillis(vaultProperties.getConnectionTimeout()),
      ofMillis(vaultProperties.getReadTimeout())
    );
    final SslConfiguration sslConfiguration = createSslConfiguration(vaultProperties.getSsl());
    return create(clientOptions, sslConfiguration);
  }
  
  private static SslConfiguration createSslConfiguration(final VaultProperties.Ssl ssl) {
    if (ssl == null) {
      return SslConfiguration.unconfigured();
    } else {
      SslConfiguration.KeyStoreConfiguration keyStore = unconfigured();
      SslConfiguration.KeyStoreConfiguration trustStore = unconfigured();
      if (ssl.getKeyStore() != null) {
        if (hasText(ssl.getKeyStorePassword())) {
          keyStore = of(ssl.getKeyStore(),
            ssl.getKeyStorePassword().toCharArray());
        } else {
          keyStore = of(ssl.getKeyStore());
        }
      }
      
      if (ssl.getTrustStore() != null) {
        if (hasText(ssl.getTrustStorePassword())) {
          trustStore = of(ssl.getTrustStore(),
            ssl.getTrustStorePassword().toCharArray());
        } else {
          trustStore = of(ssl.getTrustStore());
        }
      }
      
      return new SslConfiguration(keyStore, trustStore);
    }
  }
  
  private static VaultEndpoint createVaultEndpoint(final VaultProperties vaultProperties) {
    if (hasText(vaultProperties.getUri())) {
      return VaultEndpoint.from(URI.create(vaultProperties.getUri()));
    } else {
      final VaultEndpoint vaultEndpoint = new VaultEndpoint();
      vaultEndpoint.setHost(vaultProperties.getHost());
      vaultEndpoint.setPort(vaultProperties.getPort());
      vaultEndpoint.setScheme(vaultProperties.getScheme());
      return vaultEndpoint;
    }
  }
}
