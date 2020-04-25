package com.github.axiopisty.scvdd.restapi;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.management.ManagementFactory;

@SpringBootApplication
@Log4j2
public class Main {
  public static void main(String... args) {
    final String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
    System.setProperty("PID", pid);
    try {
      SpringApplication.run(Main.class, args);
    } catch (Throwable t) {
      log.fatal("rest-api terminated unexpectedly because: {}", t.getMessage(), t);
    }
  }
}
