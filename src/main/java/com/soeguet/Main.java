package com.soeguet;

import com.soeguet.nogui.NoGuiServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Main {

    public static void main(String... args) {

        Map<String, String> argumente = new HashMap<>();

        for (String arg : args) {
            String[] split = arg.split("=");
            if (split.length == 2) {
                argumente.put(split[0], split[1]);
            }
        }

        Optional<String> ip = Optional.ofNullable(argumente.get("ip"));
        Optional<String> port = Optional.ofNullable(argumente.get("port"));

        if (ip.isEmpty() || port.isEmpty()) {
            System.out.println("please provide additional arguments for ip and port. eg: ip=127.0.0.1 port=8100");
            return;
        }

        new NoGuiServer(ip.get(), Integer.parseInt(port.get()));
    }
}