/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.crossdc;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Retry;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.page.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanCacheStatistics;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanChannelStatistics;
import org.keycloak.testsuite.arquillian.InfinispanStatistics;
import org.keycloak.testsuite.arquillian.InfinispanStatistics.Constants;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class ActionTokenCrossDCTest extends AbstractAdminCrossDCTest {

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPasswordUpdatePage passwordUpdatePage;

    @Page
    protected ErrorPage errorPage;

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();

        getCleanup().addUserId(createdId);

        return createdId;
    }

    @Test
    public void sendResetPasswordEmailSuccessWorksInCrossDc(
      @JmxInfinispanCacheStatistics(dcIndex=0, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.ACTION_TOKEN_CACHE) InfinispanStatistics cacheDc0Node0Statistics,
      @JmxInfinispanCacheStatistics(dcIndex=0, dcNodeIndex=1, cacheName=InfinispanConnectionProvider.ACTION_TOKEN_CACHE) InfinispanStatistics cacheDc0Node1Statistics,
      @JmxInfinispanCacheStatistics(dcIndex=1, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.ACTION_TOKEN_CACHE) InfinispanStatistics cacheDc1Node0Statistics,
      @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {
        startBackendNode(0, 1);
        cacheDc0Node1Statistics.waitToBecomeAvailable(10, TimeUnit.SECONDS);

        Comparable originalNumberOfEntries = cacheDc0Node0Statistics.getSingleStatistics(Constants.STAT_CACHE_NUMBER_OF_ENTRIES);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername("user1");
        userRep.setEmail("user1@test.com");

        String id = createUser(userRep);

        UserResource user = realm.users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String link = MailUtils.getPasswordResetEmailLink(message);

        Retry.execute(() -> channelStatisticsCrossDc.reset(), 3, 100);

        assertSingleStatistics(cacheDc0Node0Statistics, Constants.STAT_CACHE_NUMBER_OF_ENTRIES,
          () -> driver.navigate().to(link),
          Matchers::is
        );

        passwordUpdatePage.assertCurrent();

        // Verify that there was at least one message sent via the channel
        assertSingleStatistics(channelStatisticsCrossDc, Constants.STAT_CHANNEL_SENT_MESSAGES,
          () -> passwordUpdatePage.changePassword("new-pass", "new-pass"),
          old -> greaterThan((Comparable) 0l)
        );

        // Verify that the caches are synchronized
        assertThat(cacheDc0Node0Statistics.getSingleStatistics(Constants.STAT_CACHE_NUMBER_OF_ENTRIES), greaterThan(originalNumberOfEntries));
        assertThat(cacheDc0Node0Statistics.getSingleStatistics(Constants.STAT_CACHE_NUMBER_OF_ENTRIES),
                is(cacheDc1Node0Statistics.getSingleStatistics(Constants.STAT_CACHE_NUMBER_OF_ENTRIES)));

        assertEquals("Your account has been updated.", driver.getTitle());

        disableDcOnLoadBalancer(0);
        enableDcOnLoadBalancer(1);

        driver.navigate().to(link);
        errorPage.assertCurrent();
    }

    @Ignore("KEYCLOAK-5030")
    @Test
    public void sendResetPasswordEmailAfterNewNodeAdded() throws IOException, MessagingException {
        disableDcOnLoadBalancer(1);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername("user1");
        userRep.setEmail("user1@test.com");

        String id = createUser(userRep);

        UserResource user = realm.users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail(actions);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String link = MailUtils.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", driver.getTitle());

        disableDcOnLoadBalancer(0);
        getManuallyStartedBackendNodes(1)
          .findFirst()
          .ifPresent(c -> {
              containerController.start(c.getQualifier());
              loadBalancerCtrl.enableBackendNodeByName(c.getQualifier());
          });

        Retry.execute(() -> {
            driver.navigate().to(link);
            errorPage.assertCurrent();
        }, 3, 400);
    }

}
