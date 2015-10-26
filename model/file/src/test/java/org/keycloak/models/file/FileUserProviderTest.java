package org.keycloak.models.file;

import org.junit.Test;
import org.keycloak.connections.file.FileConnectionProvider;
import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.file.assembler.InMemoryModelAssembler;

import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class FileUserProviderTest {

    @Test
    public void shouldFindExpiredUsers() throws Exception {
        //given
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        Calendar today = Calendar.getInstance();

        InMemoryModel memoryModel = InMemoryModelAssembler
                .emptyModel()
                .withExpiredUser("test", "notExpired", yesterday.getTime())
                .assemble();

        FileUserProvider fileUserProvider = createFileUserProvider(memoryModel);

        //when
        List<UserModel> users = fileUserProvider.searchForExpiredUsers(today.getTime(), memoryModel.getRealm("test"));

        //then
        assertThat(users).hasSize(1);
    }

    @Test
    public void shouldNotFindVerifiedUsers() throws Exception {
        //given
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        Calendar today = Calendar.getInstance();

        InMemoryModel memoryModel = InMemoryModelAssembler
                .emptyModel()
                .withVerifiedUser("test", "notExpired", yesterday.getTime())
                .assemble();

        FileUserProvider fileUserProvider = createFileUserProvider(memoryModel);

        //when
        List<UserModel> users = fileUserProvider.searchForExpiredUsers(today.getTime(), memoryModel.getRealm("test"));

        //then
        assertThat(users).isEmpty();
    }

    @Test
    public void shouldNotFindNotExpiredUsers() throws Exception {
        //given
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        Calendar today = Calendar.getInstance();

        InMemoryModel memoryModel = InMemoryModelAssembler
                .emptyModel()
                .withVerifiedUser("test", "notExpired", today.getTime())
                .assemble();

        FileUserProvider fileUserProvider = createFileUserProvider(memoryModel);

        //when
        List<UserModel> users = fileUserProvider.searchForExpiredUsers(yesterday.getTime(), memoryModel.getRealm("test"));

        //then
        assertThat(users).isEmpty();
    }

    private FileUserProvider createFileUserProvider(InMemoryModel memoryModel) {
        FileConnectionProvider fileConnectionProvider = mock(FileConnectionProvider.class);
        doReturn(memoryModel).when(fileConnectionProvider).getModel();
        return new FileUserProvider(mock(KeycloakSession.class), fileConnectionProvider);
    }

}