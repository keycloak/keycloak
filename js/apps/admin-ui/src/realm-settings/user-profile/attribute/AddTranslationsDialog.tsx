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
import { useEffect, useState } from "react";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { adminClient } from "../../../admin-client";
import { DEFAULT_LOCALE } from "../../../i18n/i18n";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { PaginatingTableToolbar } from "../../../components/table-toolbar/PaginatingTableToolbar";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import EffectiveMessageBundleRepresentation from "libs/keycloak-admin-client/lib/defs/effectiveMessageBundleRepresentation";
import { HelpItem } from "ui-shared";

export type AddTranslationsDialogProps = {
  autocompletedTranslationKey: string;
  onCancel: () => void;
  toggleDialog: () => void;
};

export const AddTranslationsDialog = ({
  autocompletedTranslationKey,
  onCancel,
  toggleDialog,
}: AddTranslationsDialogProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");
  const [translations, setTranslations] = useState<
    EffectiveMessageBundleRepresentation[]
  >([]);

  useEffect(() => {
    const fetchTranslations = async () => {
      try {
        const filter = {
          hasWords: [autocompletedTranslationKey],
          locale: "en",
          theme: "keycloak",
          themeType: "admin",
        };

        const translations =
          await adminClient.serverInfo.findEffectiveMessageBundles({
            realm,
            ...filter,
            locale: filter.locale || DEFAULT_LOCALE,
            source: true,
          });

        const filteredMessages =
          filter.hasWords.length > 0
            ? translations.filter((m) => filter.hasWords.includes(m.key))
            : translations;

        setTranslations(filteredMessages);
      } catch (error) {
        console.error("Error fetching translations:", error);
        setTranslations([]);
      }
    };

    fetchTranslations();
  }, []);

  const removeAllTranslations = async () => {
    try {
      await adminClient.realms.deleteRealmLocalizationTexts({
        realm,
        selectedLocale: "en",
        key: autocompletedTranslationKey,
      });

      setTranslations([]);
    } catch (error) {
      console.error("Error removing translations:", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addTranslationsModalTitle")}
      isOpen
      onClose={toggleDialog}
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
          onClick={onCancel}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Flex
        direction={{ default: "column" }}
        spaceItems={{ default: "spaceItemsNone" }}
      >
        <FlexItem width="100%">
          <TextContent>
            <Text component={TextVariants.p}>
              {t("addTranslationsModalSubTitle")}{" "}
              <strong>{t("addTranslationsModalSubTitleBolded")}</strong>
            </Text>
          </TextContent>
        </FlexItem>
        <FlexItem width="100%">
          <Form
            id="add-translation"
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
                defaultValue={autocompletedTranslationKey}
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
                      data-testid="remove-allTranslations"
                      variant="secondary"
                      isDanger
                      onClick={() => removeAllTranslations()}
                    >
                      {t("removeAllTranslations")}
                    </Button>
                    <HelpItem
                      helpText={t("removeAllTranslationsHelp")}
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
