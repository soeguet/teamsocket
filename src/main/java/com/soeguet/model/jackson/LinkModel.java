package com.soeguet.model.jackson;

public non-sealed class LinkModel extends BaseModel {

    // variables -- start
    protected String link;
    protected String comment;
    // variables -- end

    // getter & setter -- start
    public String getComment() {

        return comment;
    }

    public void setComment(final String comment) {

        this.comment = comment;
    }

    public String getLink() {

        return link;
    }

    public void setLink(final String link) {

        this.link = link;
    }
    // getter & setter -- end
}