package org.keycloak.ui.example;

import java.util.Arrays;

public class Id {

    private String[] id;

    public Id(String... id) {
        this.id = id;
    }

    public String[] getId() {
        return id;
    }

    public String getId(int i) {
        return id[i];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(id);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Id other = (Id) obj;
        if (!Arrays.equals(id, other.id))
            return false;
        return true;
    }

}
