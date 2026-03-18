import {
  ListEmptyState,
  PaginatingTableToolbar,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
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
  TextInput,
  TextVariants,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { Trans, useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";
import { useRealm } from "../../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../../context/whoami/WhoAmI";
import { beerify, localeToDisplayName } from "../../../util";
import useLocale from "../../../utils/useLocale";
import { Translation, TranslationForm } from "./TranslatableField";

type AddTranslationsDialogProps = {
  orgKey: string;
  translationKey: string;
  fieldName: string;
  toggleDialog: () => void;
  predefinedAttributes?: string[];
};

export const AddTranslationsDialog = ({
  orgKey,
  translationKey,
  fieldName,
  toggleDialog,
  predefinedAttributes,
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
  const prefix = `translation.${beerify(translationKey)}`;

  const {
    register,
    setValue,
    getValues,
    formState: { isValid },
  } = useFormContext();

  const setupForm = (translation: Translation) => {
    translation[translationKey].forEach((translation, rowIndex) => {
      const valueKey = `${prefix}.${rowIndex}.value`;
      setValue(`${prefix}.${rowIndex}.locale`, translation.locale || "");
      setValue(
        valueKey,
        getValues(valueKey) ||
          translation.value ||
          (t(orgKey) !== orgKey ? t(orgKey) : ""),
      );
    });
  };

  useFetch(
    async () => {
      const selectedLocales = combinedLocales
        .filter((l) =>
          localeToDisplayName(l, whoAmI.locale)
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
      setupForm({ [translationKey]: fetchedData });
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
            setupForm({ [translationKey]: translations });
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
          <Trans
            i18nKey="addTranslationsModalTitle"
            values={{ fieldName: t(fieldName) }}
          >
            You are able to translate the fieldName based on your locale or
            <strong>location</strong>
          </Trans>
        </FlexItem>
        <FlexItem>
          <Form id="add-translation" data-testid="addTranslationForm">
            <FormGroup label={t("translationKey")} fieldId="translationKey">
              <TextInput
                id="translationKey"
                label={t("translationKey")}
                data-testid="translation-key"
                isDisabled
                value={
                  predefinedAttributes?.includes(orgKey)
                    ? `\${${orgKey}}`
                    : `\${${translationKey}}`
                }
              />
            </FormGroup>
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
                              whoAmI.locale,
                            )}
                            {translation.locale === realm?.defaultLocale && (
                              <Label className="pf-v5-u-ml-xs" color="blue">
                                {t("defaultLanguage")}
                              </Label>
                            )}
                          </Td>
                          <Td>
                            <TextInput
                              id={`${prefix}.${index}.value`}
                              data-testid={`translation-value-${index}`}
                              {...register(`${prefix}.${index}.value`, {
                                required: {
                                  value:
                                    translation.locale === realm?.defaultLocale,
                                  message: t("required"),
                                },
                              })}
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
