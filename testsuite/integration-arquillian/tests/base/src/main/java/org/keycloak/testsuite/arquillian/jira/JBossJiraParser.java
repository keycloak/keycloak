/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.arquillian.jira;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class JBossJiraParser {

    private static final String JBOSS_TRACKER_REST_URL = "https://issues.jboss.org/rest/api/latest/issue/";

    public static boolean isIssueClosed(String issueId) {
        Status issueStatus;
        try {
            issueStatus = getIssueStatus(issueId);
        } catch (Exception e) {
            issueStatus = Status.CLOSED; //let the test run in case there is no connection
        }
        return issueStatus == Status.CLOSED || issueStatus == Status.RESOLVED;
    }

    private static Status getIssueStatus(String issueId) throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(JBOSS_TRACKER_REST_URL);
        String json = target.path(issueId).request().accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        JsonObject jsonObject = new Gson().fromJson(json, JsonElement.class).getAsJsonObject();
        String status = jsonObject.getAsJsonObject("fields").getAsJsonObject("status").get("name").getAsString();
        client.close();
        return Status.getByStatus(status);
    }
}
