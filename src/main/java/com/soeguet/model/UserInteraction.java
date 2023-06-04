package com.soeguet.model;

public class UserInteraction {

    private String username;
    private String emoji;

    public UserInteraction() {
    }

    public UserInteraction(String username, String emoji) {
        this.username = username;
        this.emoji = emoji;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Override
    public String toString() {
        return "UserInteraction{" +
                "username='" + username + '\'' +
                ", emoji='" + emoji + '\'' +
                '}';
    }
}