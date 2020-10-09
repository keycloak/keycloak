package org.keycloak.models.map.user;

import org.junit.Before;
import org.junit.Test;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import org.keycloak.credential.CredentialModel;

import java.util.List;
import java.util.stream.Collectors;

public class AbstractUserEntityCredentialsOrderTest {

    private AbstractUserEntity<Integer> user;
    
    @Before
    public void init() {
        user = new AbstractUserEntity<Integer>(1, "realmId") {};
        
        for (int i = 1; i <= 5; i++) {
            UserCredentialEntity credentialModel = new UserCredentialEntity();
            credentialModel.setId(Integer.toString(i));

            user.addCredential(credentialModel);
        }
    }

    private void assertOrder(Integer... ids) {
        List<Integer> currentList = user.getCredentials().map(entity -> Integer.valueOf(entity.getId())).collect(Collectors.toList());
        assertThat(currentList, Matchers.contains(ids));
    }

    @Test
    public void testCorrectOrder() {
        assertOrder(1, 2, 3, 4, 5);
    }

    @Test
    public void testMoveToZero() {
        user.moveCredential(2, 0);
        assertOrder(3, 1, 2, 4, 5);
    }

    @Test
    public void testMoveBack() {
        user.moveCredential(3, 1);
        assertOrder(1, 4, 2, 3, 5);
    }

    @Test
    public void testMoveForward() {
        user.moveCredential(1, 3);
        assertOrder(1, 3, 4, 2, 5);
    }

    @Test
    public void testSamePosition() {
        user.moveCredential(1, 1);
        assertOrder(1, 2, 3, 4, 5);
    }

    @Test
    public void testSamePositionZero() {
        user.moveCredential(0, 0);
        assertOrder(1, 2, 3, 4, 5);
    }

}