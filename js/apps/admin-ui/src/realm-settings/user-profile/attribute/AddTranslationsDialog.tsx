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
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { SearchIcon } from "@patternfly/react-icons";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../../context/whoami/WhoAmI";
import { adminClient } from "../../../admin-client";
import { PaginatingTableToolbar } from "../../../components/table-toolbar/PaginatingTableToolbar";
import { ListEmptyState } from "../../../components/list-empty-state/ListEmptyState";
import { useFetch } from "../../../utils/useFetch";
import { localeToDisplayName } from "../../../util";
import { DEFAULT_LOCALE } from "../../../i18n/i18n";
import { TextControl } from "ui-shared";

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
  onCancel: () => void;
  toggleDialog: () => void;
  onTranslationsAdded: (translations: Translations) => void;
};

export const AddTranslationsDialog = ({
  translationKey,
  onCancel,
  toggleDialog,
  onTranslationsAdded,
}: AddTranslationsDialogProps) => {
  const { t } = useTranslation();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const { whoAmI } = useWhoAmI();
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");

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
      setValue(`translations.${rowIndex}`, {
        locale,
        value: "",
      });
      setValue("key", translationKey);
    });
  }, [combinedLocales, translationKey, setValue]);

  const handleOk = () => {
    const formData = getValues() as Translations;

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
              {t("addTranslationsModalSubTitle")}{" "}
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
                className="pf-u-mt-md"
                data-testid="translation-key"
                isDisabled
              />
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
                        {filteredLocales.map((locale) => {
                          const rowIndex = combinedLocales.findIndex(
                            (combinedLocale) => combinedLocale === locale,
                          );
                          return (
                            <Tr key={rowIndex}>
                              <Td
                                className="pf-m-sm pf-u-px-sm"
                                dataLabel={t("supportedLanguage")}
                              >
                                <FormGroup fieldId="kc-supportedLanguage">
                                  {localeToDisplayName(
                                    locale,
                                    whoAmI.getLocale(),
                                  )}
                                  {locale === defaultLocales.toString() && (
                                    <Label className="pf-u-ml-xs" color="blue">
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
