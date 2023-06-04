package com.soeguet;

import com.soeguet.nogui.NoGuiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    logger.info("Starting server...");
    new NoGuiServer("localhost", 8100);
  }
}
