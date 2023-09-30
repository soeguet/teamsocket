package com.soeguet.model.jackson;

import java.util.Arrays;

public class PictureModel extends BaseModel {

    private byte[] picture;

    public byte[] getPicture() {

        return picture;
    }

    public void setPicture(byte[] picture) {

        this.picture = picture;
    }

    @Override
    public String toString() {

        return "PictureModel{" +
                "picture=" + Arrays.toString(picture) +
                ", id=" + id +
                ", userInteractions=" + userInteractions +
                ", localIp='" + localIp + '\'' +
                ", sender='" + sender + '\'' +
                ", time='" + time + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}