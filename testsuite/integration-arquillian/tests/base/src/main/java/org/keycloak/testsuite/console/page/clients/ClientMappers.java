package org.keycloak.testsuite.console.page.clients;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tkyjovsk
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientMappers extends Client {

    public static final String ADD_BUILTIN = "Add Builtin";

    @FindBy(tagName = "table")
    private ClientMapperTable table;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/mappers";
    }

    public ClientMapperTable mapperTable() {
        return table;
    }

    public class ClientMapperTable extends DataTable {

        public List<ProtocolMapperRepresentation> searchMappings(String searchPattern) {
            search(searchPattern);
            return getMappingsFromRows();
        }

        public void createMapper() {
            waitAjaxForBody();
            clickHeaderLink(CREATE);
        }

        public void addBuiltin() {
            waitAjaxForBody();
            clickHeaderLink(ADD_BUILTIN);
        }

        public void clickMapper(String mapperName) {
            waitAjaxForBody();
            body().findElement(By.linkText(mapperName)).click();
        }

        public void clickMapper(ProtocolMapperRepresentation mapper) {
            clickMapper(mapper.getName());
        }

        private void clickMapperActionButton(String mapperName, String buttonText) {
            waitAjaxForBody();
            clickRowActionButton(getRowByLinkText(mapperName), buttonText);
        }

        private void clickMapperActionButton(ProtocolMapperRepresentation mapper, String buttonName) {
            clickMapperActionButton(mapper.getName(), buttonName);
        }

        public void editMapper(String mapperName) {
            clickMapperActionButton(mapperName, EDIT);
        }

        public void editMapper(ProtocolMapperRepresentation mapper) {
            clickMapperActionButton(mapper, EDIT);
        }

        public void deleteMapper(String mapperName) {
            clickMapperActionButton(mapperName, DELETE);
        }

        public void deleteMapper(ProtocolMapperRepresentation mapper) {
            clickMapperActionButton(mapper, DELETE);
        }

        public ProtocolMapperRepresentation getMappingFromRow(WebElement row) {
            if (!row.isDisplayed()) {return null;} // Is that necessary?

            ProtocolMapperRepresentation mappingsRepresentation = new ProtocolMapperRepresentation();
            List<WebElement> cols = row.findElements(By.tagName("td"));


            mappingsRepresentation.setName(cols.get(0).getText());
            //mappingsRepresentation.setProtocol(cols.get(1).getText());
            mappingsRepresentation.setProtocolMapper(cols.get(2).getText());

            return mappingsRepresentation;
        }

        public List<ProtocolMapperRepresentation> getMappingsFromRows() {
            List<ProtocolMapperRepresentation> mappings = new ArrayList<ProtocolMapperRepresentation>();

            for (WebElement row : rows()) {
                ProtocolMapperRepresentation mapperRepresentation = getMappingFromRow(row);
                if (mapperRepresentation != null) {
                    mappings.add(mapperRepresentation);
                }
            }

            return mappings;
        }
    }

}
