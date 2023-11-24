package com.soeguet.controller.interfaces;

import com.soeguet.model.dtos.UpdatedReactionModelDTO;

import java.util.Deque;

public interface MessagesControllerInterface {

    Deque<String> retrieveLast100Messages();

    String onMessageFromClient(String message);

    boolean checkByteArrayForReaction(byte[] array);

    UpdatedReactionModelDTO updateDatabaseEntryWithReaction(byte[] byteData);

    void replaceDatabaseEntryWithUpdatedModel(UpdatedReactionModelDTO updatedModel);
    void sendUpdatedBaseModelToAllClients(Long databaseId);
}