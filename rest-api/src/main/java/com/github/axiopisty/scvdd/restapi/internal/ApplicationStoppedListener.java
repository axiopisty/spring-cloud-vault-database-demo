package com.github.axiopisty.scvdd.restapi.internal;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class ApplicationStoppedListener implements ApplicationListener<ContextClosedEvent> {
  @Override
  public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
    log.info("rest-api stopped");
  }
}
