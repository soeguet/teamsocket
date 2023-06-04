package com.soeguet.model;

import java.util.Objects;

public class Clients {

    private String localIpAddress;
    private String clientName;

    public Clients() {
    }

    public Clients(String localIpAddress, String clientName) {
        this.localIpAddress = localIpAddress;
        this.clientName = clientName;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public void setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String toString() {
        return "Clients{" +
                "localIpAddress='" + localIpAddress + '\'' +
                ", clientName='" + clientName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clients clients = (Clients) o;
        return Objects.equals(localIpAddress, clients.localIpAddress) && Objects.equals(clientName, clients.clientName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localIpAddress, clientName);
    }
}
