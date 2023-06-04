package com.soeguet.model;

import java.util.List;
import java.util.Objects;

public class ClientsList {

    private List<Clients> clientsList;

    public ClientsList(List<Clients> clientsList) {
        this.clientsList = clientsList;
    }

    public ClientsList() {
    }

    public List<Clients> getClientsList() {
        return clientsList;
    }

    public void setClientsList(List<Clients> clientsList) {
        this.clientsList = clientsList;
    }

    @Override
    public String toString() {
        return "ClientList{" +
                "clientsList=" + clientsList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientsList that = (ClientsList) o;
        return Objects.equals(clientsList, that.clientsList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientsList);
    }
}
