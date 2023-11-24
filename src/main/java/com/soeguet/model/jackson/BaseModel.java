package com.soeguet.model.jackson;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.soeguet.model.UserInteraction;

import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
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
    protected QuoteModel<? extends BaseModel> quotedMessage;
    // variables -- end

    public Long getId() {

        return id;
    }

    public void setId(final Long id) {

        this.id = id;
    }

    public String getSubclass() {

        return subclass;
    }

    public void setSubclass(final String subclass) {

        this.subclass = subclass;
    }

    public byte getMessageType() {

        return messageType;
    }

    public void setMessageType(final byte messageType) {

        this.messageType = messageType;
    }

    public List<UserInteraction> getUserInteractions() {

        return userInteractions;
    }

    public void setUserInteractions(final List<UserInteraction> userInteractions) {

        this.userInteractions = userInteractions;
    }

    public String getSender() {

        return sender;
    }

    public void setSender(final String sender) {

        this.sender = sender;
    }

    public String getTime() {

        return time;
    }

    public void setTime(final String time) {

        this.time = time;
    }

    public QuoteModel<? extends BaseModel> getQuotedMessage() {

        return quotedMessage;
    }

    public void setQuotedMessage(final QuoteModel<? extends BaseModel> quotedMessage) {

        this.quotedMessage = quotedMessage;
    }
}