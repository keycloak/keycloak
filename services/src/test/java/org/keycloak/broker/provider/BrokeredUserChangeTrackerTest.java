package org.keycloak.broker.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.models.UserModel;
import org.keycloak.storage.adapter.InMemoryUserAdapter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BrokeredUserChangeTrackerTest {

    private UserModel user;

    @Before
    public void createUser() {
        user = new InMemoryUserAdapter(null, null, "user-id");
        user.setUsername("user");
        user.setEmail("user@example.org");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setAttribute("department", new ArrayList<>(List.of("sales", "marketing")));
    }

    @Test
    public void testNoChanges() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        Assert.assertEquals(Map.of(), tracker.getChanges());
    }

    @Test
    public void testNormalizedValuesNotReportedAsChanged() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        tracker.setEmail("User@Example.org");
        tracker.setUsername("USER");
        tracker.setFirstName("First");
        tracker.setAttribute("department", new ArrayList<>(List.of("marketing", "sales")));

        Assert.assertEquals(Map.of(), tracker.getChanges());
    }

    @Test
    public void testChangedPropertiesAndAttributesReported() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        tracker.setUsername("renamed");
        tracker.setEmail("other@example.org");
        tracker.setFirstName("Changed");
        tracker.setAttribute("department", new ArrayList<>(List.of("sales")));
        tracker.setSingleAttribute("costCenter", "42");

        Assert.assertEquals(Map.of(
                "costCenter", List.of(),
                "department", List.of("sales", "marketing"),
                UserModel.USERNAME, List.of("user"),
                UserModel.EMAIL, List.of("user@example.org"),
                UserModel.FIRST_NAME, List.of("First")), tracker.getChanges());
    }

    @Test
    public void testRemovedAttributesReported() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        tracker.removeAttribute("department");
        tracker.setLastName(null);

        Assert.assertEquals(Map.of(
                "department", List.of("sales", "marketing"),
                UserModel.LAST_NAME, List.of("Last")), tracker.getChanges());
    }

    @Test
    public void testBlankValuesIgnored() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        tracker.setSingleAttribute("costCenter", "");
        tracker.setAttribute("department", new ArrayList<>(List.of("sales", "marketing", " ")));

        Assert.assertEquals(Map.of(), tracker.getChanges());
    }

    @Test
    public void testRestoredValuesNotReported() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        tracker.setFirstName("Changed");
        tracker.setFirstName("First");

        Assert.assertEquals(Map.of(), tracker.getChanges());
    }

    @Test
    public void testChangesNotMadeThroughTrackerNotReported() {
        BrokeredUserChangeTracker tracker = BrokeredUserChangeTracker.track(user);

        // e.g. a user federated from LDAP, whose values are read from LDAP once the user is updated
        user.setLastName("Changed in LDAP");
        tracker.setFirstName("Changed");

        Assert.assertEquals(Map.of(UserModel.FIRST_NAME, List.of("First")), tracker.getChanges());
    }
}
