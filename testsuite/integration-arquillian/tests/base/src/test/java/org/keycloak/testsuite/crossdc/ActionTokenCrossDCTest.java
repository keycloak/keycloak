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
import org.keycloak.common.util.Retry;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.page.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.PageUtils;
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
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanCacheStatistics;
import org.keycloak.testsuite.arquillian.annotation.JmxInfinispanChannelStatistics;
import org.keycloak.testsuite.arquillian.InfinispanStatistics;
import org.keycloak.testsuite.arquillian.InfinispanStatistics.Constants;
import org.keycloak.testsuite.pages.ProceedPage;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import org.keycloak.testsuite.arquillian.CrossDCTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.InitialDcState;

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
    protected ProceedPage proceedPage;

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
    @InitialDcState(authServers = ServerSetup.ALL_NODES_IN_FIRST_DC_FIRST_NODE_IN_SECOND_DC)
    public void sendResetPasswordEmailSuccessWorksInCrossDc(
      @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.ACTION_TOKEN_CACHE) InfinispanStatistics cacheDc0Node0Statistics,
      @JmxInfinispanCacheStatistics(dc=DC.FIRST, dcNodeIndex=1, cacheName=InfinispanConnectionProvider.ACTION_TOKEN_CACHE) InfinispanStatistics cacheDc0Node1Statistics,
      @JmxInfinispanCacheStatistics(dc=DC.SECOND, dcNodeIndex=0, cacheName=InfinispanConnectionProvider.ACTION_TOKEN_CACHE) InfinispanStatistics cacheDc1Node0Statistics,
      @JmxInfinispanChannelStatistics() InfinispanStatistics channelStatisticsCrossDc) throws Exception {
        log.debug("--DC: START sendResetPasswordEmailSuccessWorksInCrossDc");
        
        cacheDc0Node1Statistics.waitToBecomeAvailable(10, TimeUnit.SECONDS);

        Comparable originalNumberOfEntries = cacheDc0Node0Statistics.getSingleStatistics(Constants.STAT_CACHE_NUMBER_OF_ENTRIES_IN_MEMORY);

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

        assertSingleStatistics(cacheDc0Node0Statistics, Constants.STAT_CACHE_NUMBER_OF_ENTRIES_IN_MEMORY,
          () -> driver.navigate().to(link),
          Matchers::is
        );

        proceedPage.assertCurrent();
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        // Verify that there was at least one message sent via the channel - Even if we did the change on DC0, the message may be sent either from DC0 or DC1. Seems it depends on the actionTokens key ownership.
        // In case that it was sent from DC1, we will receive it in DC0.
        assertStatistics(channelStatisticsCrossDc,
                () -> {
                    passwordUpdatePage.changePassword("new-pass", "new-pass");
                },
                (Map<String, Object> oldStats, Map<String, Object> newStats) -> {
                    int oldSent = ((Number) oldStats.get(Constants.STAT_CHANNEL_SENT_MESSAGES)).intValue();
                    int newSent = ((Number) newStats.get(Constants.STAT_CHANNEL_SENT_MESSAGES)).intValue();
                    int oldReceived = ((Number) oldStats.get(Constants.STAT_CHANNEL_RECEIVED_MESSAGES)).intValue();
                    int newReceived = ((Number) newStats.get(Constants.STAT_CHANNEL_RECEIVED_MESSAGES)).intValue();

                    log.infof("oldSent: %d, newSent: %d, oldReceived: %d, newReceived: %d", oldSent, newSent, oldReceived, newReceived);
                    Assert.assertTrue(newSent - oldSent > 0 || newReceived - oldReceived > 0);
                }
        );

        assertThat(PageUtils.getPageTitle(driver), containsString("Your account has been updated."));

        // Verify that there was an action token added in the node which was targetted by the link
        assertThat(cacheDc0Node0Statistics.getSingleStatistics(Constants.STAT_CACHE_NUMBER_OF_ENTRIES_IN_MEMORY), greaterThan(originalNumberOfEntries));

        disableDcOnLoadBalancer(DC.FIRST);
        enableDcOnLoadBalancer(DC.SECOND);

        // Make sure that after going to the link, the invalidated action token has been retrieved from Infinispan server cluster in the other DC
        // NOTE: Using STAT_CACHE_NUMBER_OF_ENTRIES_IN_MEMORY as it doesn't contain the items from cacheLoader (remoteCache) until they are really loaded into the cache memory. That's the
        // statistic, which is actually increased on dc1-node0 once the used actionToken is loaded to the cache (memory) from remoteCache
        assertSingleStatistics(cacheDc1Node0Statistics, Constants.STAT_CACHE_NUMBER_OF_ENTRIES_IN_MEMORY,
          () -> driver.navigate().to(link),
          Matchers::greaterThan
        );

        errorPage.assertCurrent();
        log.debug("--DC: END sendResetPasswordEmailSuccessWorksInCrossDc");
    }

    @Test
    @InitialDcState(authServers = ServerSetup.FIRST_NODE_IN_FIRST_DC)
    public void sendResetPasswordEmailAfterNewNodeAdded() throws IOException, MessagingException {
        log.debug("--DC: START sendResetPasswordEmailAfterNewNodeAdded");
        disableDcOnLoadBalancer(DC.SECOND);

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

        proceedPage.assertCurrent();
        proceedPage.clickProceedLink();
        passwordUpdatePage.assertCurrent();

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", PageUtils.getPageTitle(driver));

        disableDcOnLoadBalancer(DC.FIRST);
        CrossDCTestEnricher.startAuthServerBackendNode(DC.SECOND, 1);
        CrossDCTestEnricher.stopAuthServerBackendNode(DC.FIRST, 0);
        enableLoadBalancerNode(DC.SECOND, 1);

        Retry.execute(() -> {
            driver.navigate().to(link);
            errorPage.assertCurrent();
        }, 3, 400);

        log.debug("--DC: END sendResetPasswordEmailAfterNewNodeAdded");
    }

}
