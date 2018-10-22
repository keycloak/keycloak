package org.keycloak.testsuite.console.page.clients.mappers;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

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

        @FindBy(xpath = "//button[text() = 'Add selected']")
        private WebElement addSelectedButton;
        
        public List<ProtocolMapperRepresentation> searchMappings(String searchPattern) {
            search(searchPattern);
            return getMappingsFromRows();
        }

        public void createMapper() {
            clickHeaderLink(CREATE);
        }

        public void addBuiltin() {
            clickHeaderLink(ADD_BUILTIN);
        }

        public void clickMapper(String mapperName) {
            body().findElement(By.linkText(mapperName)).click();
        }

        public void clickMapper(ProtocolMapperRepresentation mapper) {
            clickMapper(mapper.getName());
        }

        private void clickMapperActionButton(String mapperName, String buttonText) {
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
        
        public void checkBuiltinMapper(String mapperName) {
            body().findElement(By.xpath("//td[text() = '" + mapperName + "']/..//input")).click();
        }
        
        public void clickAddSelectedBuiltinMapper() {
            addSelectedButton.click();
        }

        public ProtocolMapperRepresentation getMappingFromRow(WebElement row) {
            if (!row.isDisplayed()) {return null;} // Is that necessary?

            ProtocolMapperRepresentation mappingsRepresentation = new ProtocolMapperRepresentation();
            List<WebElement> cols = row.findElements(By.tagName("td"));


            mappingsRepresentation.setName(getTextFromElement(cols.get(0)));
            //mappingsRepresentation.setProtocol(cols.get(1).getText());
            mappingsRepresentation.setProtocolMapper(getTextFromElement(cols.get(2)));

            return mappingsRepresentation;
        }

        public List<ProtocolMapperRepresentation> getMappingsFromRows() {
            List<ProtocolMapperRepresentation> mappings = new ArrayList<>();

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
