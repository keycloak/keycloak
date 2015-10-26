package org.keycloak.services.scheduled;

import org.junit.Test;
import org.keycloak.Config;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ClearExpiredUserRegistrationsTest {

    @Test
    public void shouldBeEnabledByDefault() throws Exception {
        //given
        Config.init(new Config.SystemPropertiesConfigProvider());
        ClearExpiredUserRegistrations testedTask = new ClearExpiredUserRegistrations();

        //when
        boolean isEnabled = testedTask.isEnabled();

        //then
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void shouldBeDisabledByConfiguration() throws Exception {
        //given
        Config.Scope scope = mock(Config.Scope.class);
        Config.ConfigProvider configProvider = mock(Config.ConfigProvider.class);

        doReturn(scope).when(configProvider).scope(eq("user"));
        doReturn(false).when(scope).getBoolean(eq("enableExpiredUsersEviction"), anyBoolean());
        Config.init(configProvider);

        ClearExpiredUserRegistrations testedTask = new ClearExpiredUserRegistrations();

        //when
        boolean isEnabled = testedTask.isEnabled();

        //then
        assertThat(isEnabled).isFalse();
    }

    @Test
    public void shouldExpireInAMonthByDefault() throws Exception {
        //given
        Calendar thirtyOneDaysAgo = Calendar.getInstance();
        thirtyOneDaysAgo.add(Calendar.DAY_OF_YEAR, -29);

        Calendar twentyNineDaysAgo = Calendar.getInstance();
        twentyNineDaysAgo.add(Calendar.DAY_OF_YEAR, -31);

        Config.init(new Config.SystemPropertiesConfigProvider());

        ClearExpiredUserRegistrations testedTask = new ClearExpiredUserRegistrations();

        //when
        Date expirationDate = testedTask.getExpirationTime();

        //than
        assertThat(expirationDate).isBetween(twentyNineDaysAgo.getTime(), thirtyOneDaysAgo.getTime());
    }

    @Test
    public void shouldExpireInConfiguredTime() throws Exception {
        //given
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        Config.Scope scope = mock(Config.Scope.class);
        Config.ConfigProvider configProvider = mock(Config.ConfigProvider.class);

        doReturn(scope).when(configProvider).scope(eq("user"));
        doReturn(0).when(scope).getInt(eq("expiredUsersEvictionTimeInDays"), anyInt());
        Config.init(configProvider);

        ClearExpiredUserRegistrations testedTask = new ClearExpiredUserRegistrations();

        //when
        Date expirationDate = testedTask.getExpirationTime();

        //than
        assertThat(expirationDate).isBetween(yesterday.getTime(), tomorrow.getTime());
    }

}