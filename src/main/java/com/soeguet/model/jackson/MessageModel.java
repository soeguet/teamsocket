package com.soeguet.model.jackson;

public non-sealed class MessageModel extends BaseModel {

    // variables -- start
    protected String message;
    // variables -- end

    // getter & setter -- start
    public String getMessage() {

        return message;
    }

    public void setMessage(final String message) {

        this.message = message;
    }
    // getter & setter -- end
}