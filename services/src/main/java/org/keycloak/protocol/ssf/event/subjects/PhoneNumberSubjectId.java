package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-phone-number-identifier-for
 */
public class PhoneNumberSubjectId extends SubjectId {

    public static final String TYPE = "phone_number";

    @JsonProperty("phone_number")
    protected String phoneNumber;

    public PhoneNumberSubjectId() {
        super(TYPE);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return "PhoneNumberSubjectId{" +
               "phoneNumber='" + phoneNumber + '\'' +
               '}';
    }
}
