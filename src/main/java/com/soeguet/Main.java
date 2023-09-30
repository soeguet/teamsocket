package com.soeguet;

import com.soeguet.nogui.NoGuiServer;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String... args) {

        Map<String, String> argumente = new HashMap<>();

        for (String arg : args) {
            String[] split = arg.split("=");
            if (split.length == 2) {
                argumente.put(split[0], split[1]);
            }
        }

        String ip = argumente.get("ip");
        String port = argumente.get("port");

        if (ip == null || port == null) {
            System.out.println("please provide additional arguments for ip and port. eg: ip=127.0.0.1 port=8100");
            return;
        }

        new NoGuiServer(ip, Integer.parseInt(port));
    }
}