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
import { Controller, useForm } from "react-hook-form";
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

type Translations = {
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
  const { handleSubmit, control } = useForm<{
    key: string;
    translations: Translations[];
  }>({
    mode: "onChange",
  });

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
      toggleDialog();
      throw new Error(t("errorRemovingTranslations"));
    }
  };

  const save = async (formData: {
    key: string;
    translations: Translations[];
  }) => {
    console.log("formData >>> ", formData);
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
            onSubmit={handleSubmit(save)}
          >
            <FormGroup
              className="pf-u-mt-md"
              label={t("translationKey")}
              fieldId="kc-translation-key"
            >
              <Controller
                name="key"
                control={control}
                defaultValue={translationKey}
                render={() => (
                  <KeycloakTextInput
                    id="kc-translation-key"
                    defaultValue={translationKey}
                    aria-label={t("translationKey")}
                    data-testid="translation-key"
                  />
                )}
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
                            <FormGroup fieldId="kc-supportedLanguage">
                              {localeToDisplayName(locale, whoAmI.getLocale())}
                              {locale === defaultLocales.toString() && (
                                <Label className="pf-u-ml-xs" color="blue">
                                  {t("defaultLanguage")}
                                </Label>
                              )}
                            </FormGroup>
                          </Td>
                          <Td>
                            <FormGroup
                              fieldId="kc-translationValue"
                              helperText={
                                locale === defaultLocales.toString() &&
                                t("addTranslationDialogHelperText")
                              }
                              isRequired
                              helperTextInvalid={t("required")}
                            >
                              <Controller
                                name={`translations.${rowIndex}`}
                                control={control}
                                rules={{ required: true }}
                                render={({ field }) => (
                                  <KeycloakTextInput
                                    id="translationValue"
                                    aria-label={t("translationValue")}
                                    onChange={(e) => {
                                      const updatedTranslation = {
                                        locale,
                                        value: (e.target as HTMLInputElement)
                                          .value,
                                      };
                                      field.onChange(updatedTranslation);
                                    }}
                                  />
                                )}
                              />
                            </FormGroup>
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
