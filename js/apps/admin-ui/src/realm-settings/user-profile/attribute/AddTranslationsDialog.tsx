import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import {
  ListEmptyState,
  PaginatingTableToolbar,
  TextControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Flex,
  FlexItem,
  Form,
  Label,
  Modal,
  ModalVariant,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../../context/whoami/WhoAmI";
import { i18n } from "../../../i18n/i18n";
import { localeToDisplayName } from "../../../util";
import useLocale from "../../../utils/useLocale";

export type TranslationsType =
  | "displayName"
  | "displayHeader"
  | "displayDescription";

type TranslationForm = {
  locale: string;
  value: string;
};

export type Translations = {
  key: string;
  translations: TranslationForm[];
};

export type AddTranslationsDialogProps = {
  translationKey: string;
  fieldName: TranslationsType;
  toggleDialog: () => void;
};

type SaveTranslationsProps = {
  adminClient: KeycloakAdminClient;
  realmName: string;
  translationsData: Translations;
};

export const saveTranslations = async ({
  adminClient,
  realmName,
  translationsData: { key, translations },
}: SaveTranslationsProps) => {
  await Promise.all(
    translations
      .filter((translation) => translation.value.trim() !== "")
      .map((translation) =>
        adminClient.realms.addLocalization(
          {
            realm: realmName,
            selectedLocale: translation.locale,
            key,
          },
          translation.value,
        ),
      ),
  );
  i18n.reloadResources();
};

export const AddTranslationsDialog = ({
  translationKey,
  fieldName,
  toggleDialog,
}: AddTranslationsDialogProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const combinedLocales = useLocale();
  const { whoAmI } = useWhoAmI();
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [filter, setFilter] = useState("");
  const [translations, setTranslations] = useState<TranslationForm[]>([]);

  const {
    setValue,
    formState: { isValid },
  } = useFormContext<Translations>();

  const setupForm = (translation: Translations) => {
    setValue("key", translation.key);
    translation.translations.forEach((translation, rowIndex) => {
      setValue(`translations.${rowIndex}.locale`, translation.locale || "");
      setValue(`translations.${rowIndex}.value`, translation.value || "");
    });
  };

  useFetch(
    async () => {
      const selectedLocales = combinedLocales
        .filter((l) =>
          localeToDisplayName(l, whoAmI.getLocale())
            ?.toLocaleLowerCase(realm?.defaultLocale)
            ?.includes(filter.toLocaleLowerCase(realm?.defaultLocale)),
        )
        .slice(first, first + max + 1);

      const results = await Promise.all(
        selectedLocales.map((selectedLocale) =>
          adminClient.realms.getRealmLocalizationTexts({
            realm: realmName,
            selectedLocale,
          }),
        ),
      );

      return results.map((result, index) => ({
        locale: selectedLocales[index],
        value: result[translationKey],
      }));
    },
    (fetchedData) => {
      setTranslations(fetchedData);
      setupForm({ key: translationKey, translations: fetchedData });
    },
    [combinedLocales, first, max, filter],
  );

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
          form="add-translation"
          isDisabled={!isValid}
          onClick={toggleDialog}
        >
          {t("addTranslationDialogOkBtn")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancelTranslationBtn"
          variant="link"
          onClick={() => {
            setupForm({ key: translationKey, translations });
            toggleDialog();
          }}
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
              {t("addTranslationsModalSubTitle", { fieldName })}
              <strong>{t("addTranslationsModalSubTitleBolded")}</strong>
            </Text>
          </TextContent>
        </FlexItem>
        <FlexItem>
          <Form id="add-translation" data-testid="addTranslationForm">
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
              >
                {translations.length === 0 && filter && (
                  <ListEmptyState
                    hasIcon
                    icon={SearchIcon}
                    isSearchVariant
                    message={t("noSearchResults")}
                    instructions={t("noLanguagesSearchResultsInstructions")}
                  />
                )}
                {translations.length !== 0 && (
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
                      </Tr>
                    </Thead>
                    <Tbody>
                      {translations.slice(0, max).map((translation, index) => (
                        <Tr key={index}>
                          <Td dataLabel={t("supportedLanguage")}>
                            {localeToDisplayName(
                              translation.locale,
                              whoAmI.getLocale(),
                            )}
                            {translation.locale === realm?.defaultLocale && (
                              <Label className="pf-v5-u-ml-xs" color="blue">
                                {t("defaultLanguage")}
                              </Label>
                            )}
                          </Td>
                          <Td>
                            <TextControl
                              name={`translations.${index}.value`}
                              label={t("translationValue")}
                              data-testid={`translation-value-${index}`}
                              rules={{
                                required: {
                                  value:
                                    translation.locale === realm?.defaultLocale,
                                  message: t("required"),
                                },
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
