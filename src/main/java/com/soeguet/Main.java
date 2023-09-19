package com.soeguet;

import com.soeguet.nogui.NoGuiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    logger.info("Starting server...");

    String hostAddress = "127.0.0.1";
    int port = 8100;

    System.out.println("hostAddress: " + hostAddress + ", port: " + port);

    new NoGuiServer(hostAddress, port);
  }
}
