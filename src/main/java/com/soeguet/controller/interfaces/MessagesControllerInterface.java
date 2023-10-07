package com.soeguet.controller.interfaces;

import java.util.Deque;

public interface MessagesControllerInterface {

    Deque<String> retrieveLast100Messages();

    String onMessageFromClient(String message);
}