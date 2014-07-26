package org.keycloak.representations.idm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClaimRepresentation {
    protected boolean name;
    protected boolean username;
    protected boolean profile;
    protected boolean picture;
    protected boolean website;
    protected boolean email;
    protected boolean gender;
    protected boolean locale;
    protected boolean address;
    protected boolean phone;

    public boolean getName() {
        return name;
    }

    public void setName(boolean name) {
        this.name = name;
    }

    public boolean getUsername() {
        return username;
    }

    public void setUsername(boolean username) {
        this.username = username;
    }

    public boolean getProfile() {
        return profile;
    }

    public void setProfile(boolean profile) {
        this.profile = profile;
    }

    public boolean getPicture() {
        return picture;
    }

    public void setPicture(boolean picture) {
        this.picture = picture;
    }

    public boolean getWebsite() {
        return website;
    }

    public void setWebsite(boolean website) {
        this.website = website;
    }

    public boolean getEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    public boolean getGender() {
        return gender;
    }

    public void setGender(boolean gender) {
        this.gender = gender;
    }

    public boolean getLocale() {
        return locale;
    }

    public void setLocale(boolean locale) {
        this.locale = locale;
    }

    public boolean getAddress() {
        return address;
    }

    public void setAddress(boolean address) {
        this.address = address;
    }

    public boolean getPhone() {
        return phone;
    }

    public void setPhone(boolean phone) {
        this.phone = phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClaimRepresentation that = (ClaimRepresentation) o;

        if (address != that.address) return false;
        if (email != that.email) return false;
        if (gender != that.gender) return false;
        if (locale != that.locale) return false;
        if (name != that.name) return false;
        if (phone != that.phone) return false;
        if (picture != that.picture) return false;
        if (profile != that.profile) return false;
        if (username != that.username) return false;
        if (website != that.website) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (name ? 1 : 0);
        result = 31 * result + (username ? 1 : 0);
        result = 31 * result + (profile ? 1 : 0);
        result = 31 * result + (picture ? 1 : 0);
        result = 31 * result + (website ? 1 : 0);
        result = 31 * result + (email ? 1 : 0);
        result = 31 * result + (gender ? 1 : 0);
        result = 31 * result + (locale ? 1 : 0);
        result = 31 * result + (address ? 1 : 0);
        result = 31 * result + (phone ? 1 : 0);
        return result;
    }
}
