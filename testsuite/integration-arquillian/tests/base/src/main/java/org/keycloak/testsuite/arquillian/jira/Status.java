/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.arquillian.jira;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public enum Status {

    OPEN("Open"), CLOSED("Closed"), PULL_REQUEST_SENT("Pull Request Sent"), REOPENED("Reopened"),
    RESOLVED("Resolved"), CODING_IN_PROGRESS("Coding In Progress ");

    private String status;

    private Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static Status getByStatus(String status) {
        for (Status s : Status.values()) {
            if (s.getStatus().equals(status)) {
                return s;
            }
        }
        return null;
    }

}
