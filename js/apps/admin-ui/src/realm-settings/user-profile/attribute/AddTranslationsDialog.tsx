import {
  Button,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Label,
  Modal,
  ModalVariant,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { SearchIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useMemo, useState } from "react";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { adminClient } from "../../../admin-client";
import { DEFAULT_LOCALE } from "../../../i18n/i18n";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { PaginatingTableToolbar } from "../../../components/table-toolbar/PaginatingTableToolbar";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useFetch } from "../../../utils/useFetch";
import { localeToDisplayName } from "../../../util";
import { useWhoAmI } from "../../../context/whoami/WhoAmI";
import { HelpItem } from "ui-shared";

type Translation = {
  locale: string;
  value: string;
};

export type AddTranslationsDialogProps = {
  translationKey: string;
  defaultTranslationValue: string;
  onCancel: () => void;
  toggleDialog: () => void;
};

export const AddTranslationsDialog = ({
  translationKey,
  defaultTranslationValue,
  onCancel,
  toggleDialog,
}: AddTranslationsDialogProps) => {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const { whoAmI } = useWhoAmI();
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");
  const [formValues, setFormValues] = useState<Translation[]>([]);

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      if (!realm) {
        throw new Error(t("notFound"));
      }
      setRealm(realm);
    },
    [],
  );

  const defaultSupportedLocales = useMemo(() => {
    return realm?.supportedLocales!.length
      ? realm.supportedLocales
      : [DEFAULT_LOCALE];
  }, [realm]);

  const defaultLocales = useMemo(() => {
    return realm?.defaultLocale!.length ? [realm.defaultLocale] : [];
  }, [realm]);

  const combinedLocales = useMemo(() => {
    return Array.from(new Set([...defaultLocales, ...defaultSupportedLocales]));
  }, [defaultLocales, defaultSupportedLocales]);

  const filteredLocales = useMemo(() => {
    return combinedLocales.filter((locale) =>
      localeToDisplayName(locale, whoAmI.getLocale())!
        .toLowerCase()
        .includes(filter.toLowerCase()),
    );
  }, [combinedLocales, filter, whoAmI]);

  const removeAllTranslations = async () => {
    try {
      await Promise.all(
        combinedLocales.map(async (locale) => {
          await adminClient.realms.deleteRealmLocalizationTexts({
            realm: realmName,
            selectedLocale: locale,
            key: translationKey,
          });
        }),
      );

      toggleDialog();
    } catch (error) {
      throw new Error(t("errorRemovingTranslations"));
    }
  };

  console.log("defaultTranslationValue", defaultTranslationValue);

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
                defaultValue={translationKey}
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
                count={combinedLocales.length}
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
                {filteredLocales.length === 0 && !filter && (
                  <ListEmptyState
                    hasIcon
                    message={t("noLanguages")}
                    instructions={t("noLanguagesInstructions")}
                  />
                )}
                {filteredLocales.length === 0 && filter && (
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
                {filteredLocales.length !== 0 && (
                  <Table
                    aria-label={t("addTranslationsDialogRowsTable")}
                    data-testid="add-translations-dialog-rows-table"
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
                    <Tbody>
                      {filteredLocales.map((locale, rowIndex) => (
                        <Tr key={rowIndex}>
                          <Td
                            className="pf-m-sm pf-u-px-sm"
                            dataLabel={t("supportedLanguage")}
                          >
                            {localeToDisplayName(locale, whoAmI.getLocale())}
                            {locale === defaultLocales.toString() && (
                              <Label className="pf-u-ml-xs" color="blue">
                                {t("defaultLanguage")}
                              </Label>
                            )}
                          </Td>
                          <Td>
                            <KeycloakTextInput
                              aria-label={t("translationValue")}
                              type="text"
                              className="pf-u-w-initial"
                              data-testid={`translationValueInput-${rowIndex}`}
                              value={
                                locale === defaultLocales.toString()
                                  ? "test"
                                  : formValues[rowIndex]?.value
                              }
                              onChange={(event) => {
                                const newFormValues = [...formValues];
                                newFormValues[rowIndex] = {
                                  locale: locale,
                                  value: (event.target as HTMLInputElement)
                                    .value,
                                };
                                setFormValues(newFormValues);
                              }}
                            />
                          </Td>
                        </Tr>
                      ))}
                    </Tbody>
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
