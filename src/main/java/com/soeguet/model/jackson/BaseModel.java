package com.soeguet.model.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soeguet.model.UserInteraction;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "messageType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MessageModel.class, name = "text"),
        @JsonSubTypes.Type(value = PictureModel.class, name = "image")
})
public abstract class BaseModel {

    Long id;
    List<UserInteraction> userInteractions;
    String localIp;
    String sender;
    String time;
    String message;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public List<UserInteraction> getUserInteractions() {

        return userInteractions;
    }

    public void setUserInteractions(List<UserInteraction> userInteractions) {

        this.userInteractions = userInteractions;
    }

    public String getLocalIp() {

        return localIp;
    }

    public void setLocalIp(String localIp) {

        this.localIp = localIp;
    }

    public String getSender() {

        return sender;
    }

    public void setSender(String sender) {

        this.sender = sender;
    }

    public String getTime() {

        return time;
    }

    public void setTime(String time) {

        this.time = time;
    }

    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {

        this.message = message;
    }

    @Override
    public String toString() {

        return "BaseModel{" +
                "id=" + id +
                ", userInteractions=" + userInteractions +
                ", localIp='" + localIp + '\'' +
                ", sender='" + sender + '\'' +
                ", time='" + time + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}