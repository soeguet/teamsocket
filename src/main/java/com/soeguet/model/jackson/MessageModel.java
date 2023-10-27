package com.soeguet.model.jackson;

import com.soeguet.model.UserInteraction;

import java.util.ArrayList;
import java.util.List;

public class MessageModel extends BaseModel {

    private String quotedMessageSender;
    private String quotedMessageTime;
    private String quotedMessageText;

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

        return "MessageModel{" + "quotedMessageSender='" + quotedMessageSender + '\'' + ", quotedMessageTime='" + quotedMessageTime + '\'' + ", " + "quotedMessageText='" + quotedMessageText + '\'' + ", id=" + id + ", userInteractions=" + userInteractions + ", localIp='" + localIp + '\'' + ", sender='" + sender + '\'' + ", time='" + time + '\'' + ", message='" + message + '\'' + ", messageType=" + messageType + '}';
    }
}