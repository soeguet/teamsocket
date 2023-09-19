package com.soeguet.model.jackson;

import com.soeguet.model.UserInteraction;
import com.soeguet.util.MessageTypes;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessageModel extends BaseModel {

    private byte messageType;
    private String quotedMessageSender;
    private String quotedMessageTime;
    private String quotedMessageText;

    public MessageModel(String sender, String message) {

        this((byte) MessageTypes.NORMAL, sender, message, null, null, null);
    }

    public MessageModel(Long id) {

        this(id, (byte) MessageTypes.DELETED, null, null, null, null, "delete by user", null, null, null);
    }

    public MessageModel(byte messageType, String sender, String message) {

        this(messageType, sender, message, null, null, null);
    }

    public MessageModel(String sender, String message, String quotedMessageSender, String quotedMessageTime, String quotedMessageText) {

        this((byte) MessageTypes.NORMAL, sender, message, quotedMessageSender, quotedMessageTime, quotedMessageText);
    }

    public MessageModel(byte messageType, String sender, String message, String quotedMessageSender, String quotedMessageTime, String quotedMessageText) {

        this(null, messageType, null, null, sender, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), message, quotedMessageSender, quotedMessageTime, quotedMessageText);
    }

    // for updating messages
    public MessageModel(Long id, byte messageType, List<UserInteraction> userInteraction, String localIp, String sender, String time, String message, String quotedMessageSender, String quotedMessageTime, String quotedMessageText) {

        this.id = id;
        this.messageType = messageType;

        this.userInteractions = userInteraction;
        this.localIp = localIp;
        this.sender = sender;
        this.time = time;
        this.message = message;
        this.quotedMessageSender = quotedMessageSender;
        this.quotedMessageTime = quotedMessageTime;
        this.quotedMessageText = quotedMessageText;
    }

    public MessageModel() {

    }

    public byte getMessageType() {

        return messageType;
    }

    public void setMessageType(byte messageType) {

        this.messageType = messageType;
    }

    public String getQuotedMessageSender() {

        return quotedMessageSender;
    }

    public void setQuotedMessageSender(String quotedMessageSender) {

        this.quotedMessageSender = quotedMessageSender;
    }

    public String getQuotedMessageTime() {

        return quotedMessageTime;
    }

    public void setQuotedMessageTime(String quotedMessageTime) {

        this.quotedMessageTime = quotedMessageTime;
    }

    public String getQuotedMessageText() {

        return quotedMessageText;
    }

    public void setQuotedMessageText(String quotedMessageText) {

        this.quotedMessageText = quotedMessageText;
    }

    public List<UserInteraction> addUserInteractions(String name, String emoji) {

        if (this.userInteractions == null) {
            this.userInteractions = new ArrayList<>();
        }
        this.userInteractions.add(new UserInteraction(name, emoji));
        return this.userInteractions;
    }

    @Override
    public String toString() {

        return "MessageModel{" +
                "messageType=" + messageType +
                ", quotedMessageSender='" + quotedMessageSender + '\'' +
                ", quotedMessageTime='" + quotedMessageTime + '\'' +
                ", quotedMessageText='" + quotedMessageText + '\'' +
                ", id=" + id +
                ", userInteractions=" + userInteractions +
                ", localIp='" + localIp + '\'' +
                ", sender='" + sender + '\'' +
                ", time='" + time + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}