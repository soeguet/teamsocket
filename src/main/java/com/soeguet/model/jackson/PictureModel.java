package com.soeguet.model.jackson;

public class PictureModel extends BaseModel {

    private byte[] picture;

    public byte[] getPicture() {

        return picture;
    }

    public void setPicture(byte[] picture) {

        this.picture = picture;
    }
}