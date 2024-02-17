/* eslint-disable @typescript-eslint/no-unnecessary-condition */
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
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
  ValidatedOptions,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { SearchIcon } from "@patternfly/react-icons";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../../context/whoami/WhoAmI";
import { adminClient } from "../../../admin-client";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { PaginatingTableToolbar } from "../../../components/table-toolbar/PaginatingTableToolbar";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useFetch } from "../../../utils/useFetch";
import { localeToDisplayName } from "../../../util";
import { DEFAULT_LOCALE } from "../../../i18n/i18n";
import { HelpItem } from "ui-shared";

type TranslationsForm = {
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
  const [isWarning, setIsWarning] = useState(false);
  const { control, getValues, handleSubmit, setValue } = useForm<{
    key: string;
    translations: TranslationsForm[];
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

  useEffect(() => {
    combinedLocales.forEach((locale, rowIndex) => {
      const defaultValue =
        locale === defaultLocales.toString() ? defaultTranslationValue : "";
      setValue(`translations.${rowIndex}`, {
        locale,
        value: defaultValue,
      });
      setValue("key", translationKey);
    });
  }, [
    combinedLocales,
    defaultLocales,
    defaultTranslationValue,
    translationKey,
    setValue,
  ]);

  const removeAllTranslations = async () => {
    const formData = getValues();

    try {
      await Promise.all(
        combinedLocales.map(async (locale) => {
          try {
            const response = await adminClient.realms.getRealmLocalizationTexts(
              {
                realm: realmName,
                selectedLocale: locale,
              },
            );

            if (response) {
              await adminClient.realms.deleteRealmLocalizationTexts({
                realm: realmName,
                selectedLocale: locale,
                key: formData.key,
              });
            }
          } catch (error) {
            console.error(`Error removing translations for ${locale}`);
          }
        }),
      );
      toggleDialog();
    } catch (error) {
      console.error(`Error removing translations: ${error}`);
    }
  };

  const save = async () => {
    const formData = getValues();

    try {
      const nonEmptyTranslations = formData.translations
        .filter((translation) => translation.value.trim() !== "")
        .map(async (translation) => {
          try {
            await adminClient.realms.addLocalization(
              {
                realm: realmName,
                selectedLocale: translation.locale,
                key: formData.key,
              },
              translation.value,
            );
          } catch (error) {
            console.error(`Error saving translation for ${translation.locale}`);
          }
        });

      await Promise.all(nonEmptyTranslations);

      toggleDialog();
    } catch (error) {
      console.error(`Error saving translations: ${error}`);
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
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
          isDisabled={isWarning}
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
        <FlexItem>
          <TextContent>
            <Text component={TextVariants.p}>
              {t("addTranslationsModalSubTitle")}{" "}
              <strong>{t("addTranslationsModalSubTitleBolded")}</strong>
            </Text>
          </TextContent>
        </FlexItem>
        <FlexItem>
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
                render={({ field }) => (
                  <KeycloakTextInput
                    id="kc-translation-key"
                    {...field}
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
                    instructions={t("noLanguagesSearchResultsInstructions")}
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
                            {locale === defaultLocales.toString() && (
                              <FormGroup
                                fieldId="kc-translationValue"
                                helperText={
                                  locale === defaultLocales.toString() &&
                                  t("addTranslationDialogHelperText")
                                }
                                helperTextInvalid={t("required")}
                                isRequired
                              >
                                <Controller
                                  name={`translations.${rowIndex}`}
                                  control={control}
                                  rules={{ required: true }}
                                  render={({ field }) => (
                                    <KeycloakTextInput
                                      id="translationValue"
                                      {...field.value}
                                      aria-label={t("translationValue")}
                                      data-testid="translation-value"
                                      validated={
                                        isWarning
                                          ? ValidatedOptions.warning
                                          : "default"
                                      }
                                      onChange={(e) => {
                                        const updatedTranslation = {
                                          locale,
                                          value: (e.target as HTMLInputElement)
                                            .value,
                                        };
                                        setValue(
                                          `translations.${rowIndex}`,
                                          updatedTranslation,
                                        );
                                        setIsWarning(
                                          updatedTranslation.value === "",
                                        );
                                      }}
                                    />
                                  )}
                                />
                              </FormGroup>
                            )}
                            {locale !== defaultLocales.toString() && (
                              <FormGroup fieldId="kc-translationValue">
                                <Controller
                                  name={`translations.${rowIndex}`}
                                  control={control}
                                  render={({ field }) => (
                                    <KeycloakTextInput
                                      id="translationValue"
                                      {...field.value}
                                      aria-label={t("translationValue")}
                                      data-testid="translation-value"
                                      onChange={(e) => {
                                        const updatedTranslation = {
                                          locale,
                                          value: (e.target as HTMLInputElement)
                                            .value,
                                        };
                                        setValue(
                                          `translations.${rowIndex}`,
                                          updatedTranslation,
                                        );
                                      }}
                                    />
                                  )}
                                />
                              </FormGroup>
                            )}
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
