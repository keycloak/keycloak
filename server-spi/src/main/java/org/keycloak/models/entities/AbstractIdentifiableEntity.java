package org.keycloak.models.entities;

/**
 * Base for the identifiable entity
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractIdentifiableEntity {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (this.id == null) return false;

        if (o == null || getClass() != o.getClass()) return false;

        AbstractIdentifiableEntity that = (AbstractIdentifiableEntity) o;

        if (!getId().equals(that.getId())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [ id=%s ]", getClass().getSimpleName(), getId());
    }
}
