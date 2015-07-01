/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.arquillian.jira;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;

import static org.keycloak.testsuite.arquillian.jira.JBossJiraParser.isIssueClosed;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class JiraTestExecutionDecider implements TestExecutionDecider {

    private static Map<String, Boolean> cache = new HashMap<String, Boolean>();

    @Override
    public ExecutionDecision decide(Method method) {
        Jira jiraAnnotation = method.getAnnotation(Jira.class);
        if (jiraAnnotation != null) {
            boolean executeTest = true;
            String[] issueIds = getIssuesId(jiraAnnotation.value());
            for (String issueId : issueIds) {
                if (cache.containsKey(issueId)) {
                    executeTest = cache.get(issueId);
                } else {
                    if (isIssueClosed(issueId)) {
                        cache.put(issueId, true);
                    } else {
                        executeTest = false;
                        cache.put(issueId, false);
                    }
                }
            }

            if (executeTest) {
                return ExecutionDecision.execute();
            } else {
                return ExecutionDecision.dontExecute("Issue is still opened, therefore skipping the test " + method.getName());
            }
        }
        return ExecutionDecision.execute();
    }

    private String[] getIssuesId(String value) {
        return value.replaceAll("\\s+", "").split(",");
    }

    @Override
    public int precedence() {
        return 0;
    }

}
