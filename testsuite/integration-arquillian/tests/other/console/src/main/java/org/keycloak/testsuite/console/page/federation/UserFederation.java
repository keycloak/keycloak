package org.keycloak.testsuite.console.page.federation;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author fkiss
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UserFederation extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/user-federation";
    }

    @FindByJQuery("select[ng-model*='selectedProvider']")
    private Select addProviderSelect;

    @FindBy(xpath = "//div[./h1/span[text()='User Federation']]/table")
    private FederationsTable federationsTable;

    public FederationsTable table() {
        return federationsTable;
    }

    public void addFederation(String provider) {
        addProviderSelect.selectByVisibleText(provider);
    }

    public boolean hasProvider(String provider) {
        return UIUtils.selectContainsOption(addProviderSelect, provider);
    }

    public class FederationsTable extends DataTable {
        @FindBy(xpath = "//div[@class='modal-dialog']")
        private ModalDialog modalDialog;

        public void editFederation(String federation) {
            clickRowActionButton(getRowByLinkText(federation), "Edit");
        }

        public void removeFederation(String federation) {
            clickRowActionButton(getRowByLinkText(federation), "Delete");
            modalDialog.confirmDeletion();
        }

        public int getFederationsCount() {
            return rows().size();
        }
    }

}
