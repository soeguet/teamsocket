package com.soeguet.model.jackson;

public non-sealed class PictureModel extends BaseModel {

    // variables -- start
    protected byte[] picture;
    protected String description;
    // variables -- end

    // constructors -- start
    public PictureModel() {

    }
    // constructors -- end

    // getter & setter -- start
    public String getDescription() {

        return description;
    }

    public void setDescription(final String description) {

        this.description = description;
    }

    public byte[] getPicture() {

        return picture;
    }

    public void setPicture(byte[] picture) {

        this.picture = picture;
    }
    // getter & setter -- end
}