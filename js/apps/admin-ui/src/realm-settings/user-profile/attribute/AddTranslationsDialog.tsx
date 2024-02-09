import {
  Button,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { Table, Th, Thead, Tr } from "@patternfly/react-table";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { PaginatingTableToolbar } from "../../../components/table-toolbar/PaginatingTableToolbar";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { HelpItem } from "ui-shared";

export type AddTranslationsDialogProps = {
  toggleDialog: () => void;
};

export const AddTranslationsDialog = ({
  toggleDialog,
}: AddTranslationsDialogProps) => {
  const { t } = useTranslation();
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");

  const translations = [
    { key: "key1", value: "value1" },
    { key: "key2", value: "value2" },
  ];

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addTranslationsModalTitle")}
      isOpen
      onClose={toggleDialog}
      width={"27%"}
      actions={[
        <Button
          key="save"
          data-testid="saveTranslationBtn"
          variant="primary"
          type="submit"
          form="add-translation"
        >
          {t("save")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancelTranslationBtn"
          variant="link"
          onClick={toggleDialog}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Flex
        direction={{ default: "column" }}
        spaceItems={{ default: "spaceItemsNone" }}
      >
        <FlexItem>
          <TextContent>
            <Text component={TextVariants.p}>
              {t("addTranslationsModalSubTitle")}
            </Text>
          </TextContent>
        </FlexItem>
        <FlexItem>
          <Form
            id="add-translation"
            className="pf-u-w-25vw"
            data-testid="addTranslationForm"
            onSubmit={(e) => e.preventDefault()}
          >
            <FormGroup
              className="pf-u-mt-md"
              label={t("translationKey")}
              fieldId="kc-translation-key"
            >
              <KeycloakTextInput
                id="kc-translation-key"
                defaultValue=""
                data-testid="translation-key"
              />
            </FormGroup>
            <FlexItem>
              <TextContent>
                <Text
                  className="pf-u-font-size-sm pf-u-font-weight-bold"
                  component={TextVariants.p}
                >
                  {t("translationsTableHeading")}
                </Text>
              </TextContent>
              <PaginatingTableToolbar
                count={translations.length}
                first={first}
                max={max}
                onNextClick={setFirst}
                onPreviousClick={setFirst}
                onPerPageSelect={(first, max) => {
                  setFirst(first);
                  setMax(max);
                }}
                inputGroupName={"search"}
                inputGroupOnEnter={(search) => {
                  setFilter(search);
                  setFirst(0);
                  setMax(10);
                }}
                inputGroupPlaceholder={t("searchForLanguage")}
                toolbarItem={
                  <>
                    <Button
                      data-testid="remove-allTranslationsBtn"
                      variant="secondary"
                      isDanger
                    >
                      {t("removeAllTranslationsBtn")}
                    </Button>
                    <HelpItem
                      helpText={t("removeAllTranslationsBtnHelp")}
                      fieldLabelId="removeAllTranslationsHelpBtn"
                    />
                  </>
                }
              >
                {translations.length === 0 && !filter && (
                  <ListEmptyState
                    hasIcon
                    message={t("noTranslations")}
                    instructions={t("noTranslationsInstructions")}
                  />
                )}
                {translations.length === 0 && filter && (
                  <ListEmptyState
                    hasIcon
                    icon={SearchIcon}
                    isSearchVariant
                    message={t("noSearchResults")}
                    instructions={t(
                      "noRealmOverridesSearchResultsInstructions",
                    )}
                  />
                )}
                {translations.length !== 0 && (
                  <Table
                    aria-label={t("editableRowsTable")}
                    data-testid="editable-rows-table"
                  >
                    <Thead>
                      <Tr>
                        <Th className="pf-u-py-lg">
                          {t("supportedLanguagesTableColumnName")}
                        </Th>
                        <Th className="pf-u-py-lg">
                          {t("translationTableColumnName")}
                        </Th>
                        <Th aria-hidden="true" />
                      </Tr>
                    </Thead>
                  </Table>
                )}
              </PaginatingTableToolbar>
            </FlexItem>
          </Form>
        </FlexItem>
      </Flex>
    </Modal>
  );
};
