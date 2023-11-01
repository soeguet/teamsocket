package com.soeguet.model.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soeguet.model.UserInteraction;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "subclass")
@JsonSubTypes({@JsonSubTypes.Type(value = MessageModel.class, name = "text"), @JsonSubTypes.Type(value =
        PictureModel.class, name = "image"), @JsonSubTypes.Type(value = LinkModel.class, name = "link")})
public abstract sealed class BaseModel permits LinkModel, MessageModel, PictureModel {

    // variables -- start
    protected Long id;
    protected String subclass;
    protected byte messageType;
    protected List<UserInteraction> userInteractions;
    protected String sender;
    protected String time;
    protected String quotedMessageSender;
    protected String quotedMessageTime;
    protected String quotedMessageText;
    // variables -- end

    // overrides -- start
    @Override
    public String toString() {

        return "BaseModel{" + "id=" + id + ", subclass='" + subclass + '\'' + ", messageType=" + messageType + ", " + "userInteractions=" + userInteractions + ", sender='" + sender + '\'' + ", time='" + time + '\'' + ", " + "quotedMessageSender='" + quotedMessageSender + '\'' + ", quotedMessageTime='" + quotedMessageTime + '\'' + ", quotedMessageText='" + quotedMessageText + '\'' + '}';
    }
    // overrides -- end

    // getter & setter -- start
    public Long getId() {

        return id;
    }

    public void setId(final Long id) {

        this.id = id;
    }

    public byte getMessageType() {

        return messageType;
    }

    public void setMessageType(final byte messageType) {

        this.messageType = messageType;
    }

    public String getQuotedMessageSender() {

        return quotedMessageSender;
    }

    public void setQuotedMessageSender(final String quotedMessageSender) {

        this.quotedMessageSender = quotedMessageSender;
    }

    public String getQuotedMessageText() {

        return quotedMessageText;
    }

    public void setQuotedMessageText(final String quotedMessageText) {

        this.quotedMessageText = quotedMessageText;
    }

    public String getQuotedMessageTime() {

        return quotedMessageTime;
    }

    public void setQuotedMessageTime(final String quotedMessageTime) {

        this.quotedMessageTime = quotedMessageTime;
    }

    public String getSender() {

        return sender;
    }

    public void setSender(final String sender) {

        this.sender = sender;
    }

    public String getSubclass() {

        return subclass;
    }

    public void setSubclass(final String subclass) {

        this.subclass = subclass;
    }

    public String getTime() {

        return time;
    }

    public void setTime(final String time) {

        this.time = time;
    }

    public List<UserInteraction> getUserInteractions() {

        return userInteractions;
    }

    public void setUserInteractions(final List<UserInteraction> userInteractions) {

        this.userInteractions = userInteractions;
    }
    // getter & setter -- end
}