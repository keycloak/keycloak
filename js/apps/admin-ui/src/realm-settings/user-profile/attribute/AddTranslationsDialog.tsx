import { TextControl } from "@keycloak/keycloak-ui-shared";
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
import { SearchIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useEffect, useMemo, useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { PaginatingTableToolbar } from "../../../components/table-toolbar/PaginatingTableToolbar";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../../context/whoami/WhoAmI";
import { localeToDisplayName } from "../../../util";
import { useFetch } from "../../../utils/useFetch";
import useLocale from "../../../utils/useLocale";

export type TranslationsType =
  | "displayName"
  | "displayHeader"
  | "displayDescription";

type TranslationForm = {
  locale: string;
  value: string;
};

type Translations = {
  key: string;
  translations: TranslationForm[];
};

export type AddTranslationsDialogProps = {
  translationKey: string;
  translations: Translations;
  type: TranslationsType;
  onCancel: () => void;
  toggleDialog: () => void;
  onTranslationsAdded: (translations: Translations) => void;
};

export const AddTranslationsDialog = ({
  translationKey,
  translations,
  type,
  onCancel,
  toggleDialog,
  onTranslationsAdded,
}: AddTranslationsDialogProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const combinedLocales = useLocale();
  const { whoAmI } = useWhoAmI();
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");
  const [defaultTranslations, setDefaultTranslations] = useState<{
    [key: string]: string;
  }>({});

  const form = useForm<{
    key: string;
    translations: TranslationForm[];
  }>({
    mode: "onChange",
  });

  const {
    getValues,
    handleSubmit,
    setValue,
    formState: { isValid },
  } = form;

  const defaultLocales = useMemo(() => {
    return realm?.defaultLocale!.length ? [realm.defaultLocale] : [];
  }, [realm]);

  const filteredLocales = useMemo(() => {
    return combinedLocales.filter((locale) =>
      localeToDisplayName(locale, whoAmI.getLocale())!
        .toLowerCase()
        .includes(filter.toLowerCase()),
    );
  }, [combinedLocales, filter, whoAmI]);

  useFetch(
    async () => {
      const selectedLocales = combinedLocales.map((locale) => locale);

      const results = await Promise.all(
        selectedLocales.map((selectedLocale) =>
          adminClient.realms.getRealmLocalizationTexts({
            realm: realmName,
            selectedLocale,
          }),
        ),
      );

      const translations = results.map((result, index) => {
        const locale = selectedLocales[index];
        const value = result[translationKey];
        return {
          key: translationKey,
          translations: [{ locale, value }],
        };
      });

      const defaultValuesMap = translations.reduce((acc, translation) => {
        const locale = translation.translations[0].locale;
        const value = translation.translations[0].value;
        return { ...acc, [locale]: value };
      }, {});

      return defaultValuesMap;
    },
    (fetchedData) => {
      setDefaultTranslations((prevTranslations) => {
        if (prevTranslations !== fetchedData) {
          return fetchedData;
        }
        return prevTranslations;
      });
    },
    [combinedLocales, translationKey, realmName],
  );

  useEffect(() => {
    combinedLocales.forEach((locale, rowIndex) => {
      setValue(`translations.${rowIndex}.locale`, locale);
      setValue(
        `translations.${rowIndex}.value`,
        translations.translations.length > 0
          ? translations.translations[rowIndex].value
          : defaultTranslations[locale] || "",
      );
    });
    setValue("key", translationKey);
  }, [
    combinedLocales,
    defaultTranslations,
    translationKey,
    setValue,
    translations,
  ]);

  const handleOk = () => {
    const formData = getValues();

    const updatedTranslations = formData.translations.map((translation) => {
      if (translation.locale === filter) {
        return {
          ...translation,
          value:
            formData.translations.find((t) => t.locale === filter)?.value ?? "",
        };
      }
      return translation;
    });

    onTranslationsAdded({
      key: formData.key,
      translations: updatedTranslations,
    });

    toggleDialog();
  };

  const isRequiredTranslation = useWatch({
    control: form.control,
    name: "translations.0.value",
  });

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("addTranslationsModalTitle")}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          key="ok"
          data-testid="okTranslationBtn"
          variant="primary"
          type="submit"
          form="add-translation"
          isDisabled={!isValid || !isRequiredTranslation}
        >
          {t("addTranslationDialogOkBtn")}
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
              {type !== "displayHeader"
                ? t("addTranslationsModalSubTitleDescription")
                : t("addTranslationsModalSubTitle")}{" "}
              <strong>{t("addTranslationsModalSubTitleBolded")}</strong>
            </Text>
          </TextContent>
        </FlexItem>
        <FlexItem>
          <FormProvider {...form}>
            <Form
              id="add-translation"
              data-testid="addTranslationForm"
              onSubmit={handleSubmit(handleOk)}
            >
              <TextControl
                name="key"
                label={t("translationKey")}
                className="pf-v5-u-mt-md"
                data-testid="translation-key"
                isDisabled
              />
              <FlexItem>
                <TextContent>
                  <Text
                    className="pf-v5-u-font-size-sm pf-v5-u-font-weight-bold"
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
                          <Th className="pf-v5-u-py-lg">
                            {t("supportedLanguagesTableColumnName")}
                          </Th>
                          <Th className="pf-v5-u-py-lg">
                            {t("translationTableColumnName")}
                          </Th>
                          <Th aria-hidden="true" />
                        </Tr>
                      </Thead>
                      <Tbody>
                        {filteredLocales.map((locale, index) => {
                          const rowIndex = combinedLocales.findIndex(
                            (combinedLocale) => combinedLocale === locale,
                          );
                          return (
                            <Tr key={index}>
                              <Td
                                className="pf-m-sm pf-v5-u-px-sm"
                                dataLabel={t("supportedLanguage")}
                              >
                                <FormGroup fieldId="kc-supportedLanguage">
                                  {localeToDisplayName(
                                    locale,
                                    whoAmI.getLocale(),
                                  )}
                                  {locale === defaultLocales.toString() && (
                                    <Label
                                      className="pf-v5-u-ml-xs"
                                      color="blue"
                                    >
                                      {t("defaultLanguage")}
                                    </Label>
                                  )}
                                </FormGroup>
                              </Td>
                              <Td>
                                {locale === defaultLocales.toString() && (
                                  <TextControl
                                    name={`translations.${rowIndex}.value`}
                                    label={t("translationValue")}
                                    data-testid={`translation-value-${rowIndex}`}
                                    rules={{
                                      required: {
                                        value: true,
                                        message: t("required"),
                                      },
                                    }}
                                  />
                                )}
                                {locale !== defaultLocales.toString() && (
                                  <TextControl
                                    name={`translations.${rowIndex}.value`}
                                    label={t("translationValue")}
                                    data-testid={`translation-value-${rowIndex}`}
                                  />
                                )}
                              </Td>
                            </Tr>
                          );
                        })}
                      </Tbody>
                    </Table>
                  )}
                </PaginatingTableToolbar>
              </FlexItem>
            </Form>
          </FormProvider>
        </FlexItem>
      </Flex>
    </Modal>
  );
};
