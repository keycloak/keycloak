import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  Button,
  FormGroup,
  Grid,
  GridItem,
  PageSection,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import { GlobeRouteIcon } from "@patternfly/react-icons";
import { useEffect, useMemo, useState } from "react";
import {
  FormProvider,
  SubmitHandler,
  useForm,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";
import useLocale from "../../utils/useLocale";
import useToggle from "../../utils/useToggle";
import "../realm-settings-section.css";
import type { EditAttributesGroupParams } from "../routes/EditAttributesGroup";
import { toUserProfile } from "../routes/UserProfile";
import { useUserProfile } from "./UserProfileContext";
import {
  AddTranslationsDialog,
  TranslationsType,
} from "./attribute/AddTranslationsDialog";
import { i18n } from "../../i18n/i18n";

function parseAnnotations(input: Record<string, unknown>): KeyValueType[] {
  return Object.entries(input).reduce((p, [key, value]) => {
    if (typeof value === "string") {
      return [...p, { key, value }];
    } else {
      return [...p];
    }
  }, [] as KeyValueType[]);
}

function transformAnnotations(input: KeyValueType[]): Record<string, unknown> {
  return Object.fromEntries(
    input
      .filter((annotation) => annotation.key.length > 0)
      .map((annotation) => [annotation.key, annotation.value] as const),
  );
}

type FormFields = Required<Omit<UserProfileGroup, "annotations">> & {
  annotations: KeyValueType[];
};

type TranslationForm = {
  locale: string;
  value: string;
};

type Translations = {
  key: string;
  translations: TranslationForm[];
};

type TranslationsSets = {
  displayHeader: Translations;
  displayDescription: Translations;
};

const defaultValues: FormFields = {
  annotations: [],
  displayDescription: "",
  displayHeader: "",
  name: "",
};

export default function AttributesGroupForm() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const { config, save } = useUserProfile();
  const navigate = useNavigate();
  const combinedLocales = useLocale();
  const params = useParams<EditAttributesGroupParams>();
  const form = useForm<FormFields>({ defaultValues });
  const { addError } = useAlerts();
  const editMode = params.name ? true : false;
  const [newAttributesGroupName, setNewAttributesGroupName] = useState("");
  const [
    generatedAttributesGroupDisplayName,
    setGeneratedAttributesGroupDisplayName,
  ] = useState("");
  const [
    generatedAttributesGroupDisplayDescription,
    setGeneratedAttributesGroupDisplayDescription,
  ] = useState("");
  const [addTranslationsModalOpen, toggleModal] = useToggle();
  const regexPattern = /\$\{([^}]+)\}/;
  const [type, setType] = useState<TranslationsType>();
  const [translationsData, setTranslationsData] = useState<TranslationsSets>({
    displayHeader: {
      key: "",
      translations: [],
    },
    displayDescription: {
      key: "",
      translations: [],
    },
  });

  const matchingGroup = useMemo(
    () => config?.groups?.find(({ name }) => name === params.name),
    [config?.groups],
  );

  useEffect(() => {
    if (!matchingGroup) {
      return;
    }

    const annotations = matchingGroup.annotations
      ? parseAnnotations(matchingGroup.annotations)
      : [];

    form.reset({ ...defaultValues, ...matchingGroup, annotations });
  }, [matchingGroup]);

  useEffect(() => {
    form.setValue(
      "displayHeader",
      matchingGroup
        ? matchingGroup.displayHeader!
        : generatedAttributesGroupDisplayName,
    );
    form.setValue(
      "displayDescription",
      matchingGroup
        ? matchingGroup.displayDescription!
        : generatedAttributesGroupDisplayDescription,
    );
  }, [
    generatedAttributesGroupDisplayName,
    generatedAttributesGroupDisplayDescription,
  ]);

  useFetch(
    async () => {
      const translationsToSaveDisplayHeader: Translations[] = [];
      const translationsToSaveDisplayDescription: Translations[] = [];
      const formData = form.getValues();

      const translationsResults = await Promise.all(
        combinedLocales.map(async (selectedLocale) => {
          try {
            const translations =
              await adminClient.realms.getRealmLocalizationTexts({
                realm: realmName,
                selectedLocale,
              });

            const formattedDisplayHeaderKey = formData.displayHeader?.substring(
              2,
              formData.displayHeader.length - 1,
            );
            const formattedDisplayDescriptionKey =
              formData.displayDescription?.substring(
                2,
                formData.displayDescription.length - 1,
              );

            return {
              locale: selectedLocale,
              headerTranslation: translations[formattedDisplayHeaderKey] ?? "",
              descriptionTranslation:
                translations[formattedDisplayDescriptionKey] ?? "",
            };
          } catch (error) {
            console.error(
              `Error fetching translations for ${selectedLocale}:`,
              error,
            );
            return null;
          }
        }),
      );

      translationsResults.forEach((translationsResult) => {
        if (translationsResult) {
          const { locale, headerTranslation, descriptionTranslation } =
            translationsResult;
          translationsToSaveDisplayHeader.push({
            key: formData.displayHeader?.substring(
              2,
              formData.displayHeader.length - 1,
            ),
            translations: [
              {
                locale,
                value: headerTranslation,
              },
            ],
          });
          translationsToSaveDisplayDescription.push({
            key: formData.displayDescription?.substring(
              2,
              formData.displayDescription.length - 1,
            ),
            translations: [
              {
                locale,
                value: descriptionTranslation,
              },
            ],
          });
        }
      });

      return {
        translationsToSaveDisplayHeader,
        translationsToSaveDisplayDescription,
      };
    },
    (data) => {
      setTranslationsData({
        displayHeader: {
          key: data.translationsToSaveDisplayHeader[0].key,
          translations: data.translationsToSaveDisplayHeader.flatMap(
            (translationData) => translationData.translations,
          ),
        },
        displayDescription: {
          key: data.translationsToSaveDisplayDescription[0].key,
          translations: data.translationsToSaveDisplayDescription.flatMap(
            (translationData) => translationData.translations,
          ),
        },
      });
    },
    [combinedLocales],
  );

  const saveTranslations = async () => {
    const addLocalization = async (
      key: string,
      locale: string,
      value: string,
    ) => {
      try {
        await adminClient.realms.addLocalization(
          {
            realm: realmName,
            selectedLocale: locale,
            key: key,
          },
          value,
        );
      } catch (error) {
        console.error(
          `Error saving translation for locale ${locale}: ${error}`,
        );
      }
    };

    try {
      if (
        translationsData.displayHeader &&
        translationsData.displayHeader.translations.length > 0
      ) {
        for (const translation of translationsData.displayHeader.translations) {
          await addLocalization(
            translationsData.displayHeader.key,
            translation.locale,
            translation.value,
          );
        }
      }

      if (
        translationsData.displayDescription &&
        translationsData.displayDescription.translations.length > 0
      ) {
        for (const translation of translationsData.displayDescription
          .translations) {
          await addLocalization(
            translationsData.displayDescription.key,
            translation.locale,
            translation.value,
          );
        }
      }
    } catch (error) {
      console.error(`Error while processing translations: ${error}`);
    }
  };

  const onSubmit: SubmitHandler<FormFields> = async (values) => {
    if (!config) {
      return;
    }

    const groups = [...(config.groups ?? [])];
    const updateAt = matchingGroup ? groups.indexOf(matchingGroup) : -1;
    const updatedGroup: UserProfileGroup = {
      ...values,
      annotations: transformAnnotations(values.annotations),
    };

    if (updateAt === -1) {
      groups.push(updatedGroup);
    } else {
      groups[updateAt] = updatedGroup;
    }

    if (realm?.internationalizationEnabled) {
      const hasNonEmptyDisplayHeaderTranslations =
        translationsData.displayHeader.translations.some(
          (translation) => translation.value.trim() !== "",
        );

      const hasNonEmptyDisplayDescriptionTranslations =
        translationsData.displayDescription.translations.some(
          (translation) => translation.value.trim() !== "",
        );

      if (
        !hasNonEmptyDisplayHeaderTranslations ||
        !hasNonEmptyDisplayDescriptionTranslations
      ) {
        addError("createAttributeError", t("translationError"));
        return;
      }
    }

    const success = await save({ ...config, groups });

    if (success) {
      await saveTranslations();
      i18n.reloadResources();
      navigate(toUserProfile({ realm: realmName, tab: "attributes-group" }));
    }
  };

  const attributesGroupDisplayName = useWatch({
    control: form.control,
    name: "displayHeader",
  });

  const attributesGroupDisplayDescription = useWatch({
    control: form.control,
    name: "displayDescription",
  });

  const handleAttributesGroupNameChange = (
    event: React.FormEvent<HTMLInputElement>,
    value: string,
  ) => {
    const newDisplayName =
      value !== "" && realm?.internationalizationEnabled
        ? "${profile.attribute-group." + `${value}}`
        : "";
    const newDisplayDescription =
      value !== "" && realm?.internationalizationEnabled
        ? "${profile.attribute-group-description." + `${value}}`
        : "";
    setNewAttributesGroupName(value);
    setGeneratedAttributesGroupDisplayName(newDisplayName);
    setGeneratedAttributesGroupDisplayDescription(newDisplayDescription);
  };

  const attributesGroupDisplayPatternMatch = regexPattern.test(
    attributesGroupDisplayName || attributesGroupDisplayDescription,
  );

  const formattedAttributesGroupDisplayName =
    attributesGroupDisplayName?.substring(
      2,
      attributesGroupDisplayName.length - 1,
    );
  const formattedAttributesGroupDisplayDescription =
    attributesGroupDisplayDescription?.substring(
      2,
      attributesGroupDisplayDescription.length - 1,
    );

  const handleHeaderTranslationsAdded = (headerTranslations: Translations) => {
    setTranslationsData((prev) => ({
      ...prev,
      displayHeader: headerTranslations,
    }));
  };

  const handleDescriptionTranslationsAdded = (
    descriptionTranslations: Translations,
  ) => {
    setTranslationsData((prev) => ({
      ...prev,
      displayDescription: descriptionTranslations,
    }));
  };

  const handleToggleDialog = () => {
    toggleModal();
  };

  const groupDisplayNameKey =
    type === "displayHeader"
      ? formattedAttributesGroupDisplayName
      : `profile.attribute-group.${newAttributesGroupName}`;
  const groupDisplayDescriptionKey =
    type === "displayDescription"
      ? formattedAttributesGroupDisplayDescription
      : `profile.attribute-group-description.${newAttributesGroupName}`;

  return (
    <>
      {addTranslationsModalOpen && (
        <AddTranslationsDialog
          translationKey={
            type === "displayHeader"
              ? groupDisplayNameKey
              : groupDisplayDescriptionKey
          }
          type={
            type === "displayHeader" ? "displayHeader" : "displayDescription"
          }
          translations={
            type === "displayHeader"
              ? translationsData.displayHeader
              : translationsData.displayDescription
          }
          onTranslationsAdded={
            type === "displayHeader"
              ? handleHeaderTranslationsAdded
              : handleDescriptionTranslationsAdded
          }
          toggleDialog={handleToggleDialog}
          onCancel={() => {
            toggleModal();
          }}
        />
      )}
      <ViewHeader
        titleKey={matchingGroup ? "editGroupText" : "createGroupText"}
        divider
      />
      <PageSection variant="light" onSubmit={form.handleSubmit(onSubmit)}>
        <FormAccess isHorizontal role="manage-realm">
          <FormProvider {...form}>
            <TextControl
              name="name"
              label={t("nameField")}
              labelIcon={t("nameHintHelp")}
              isDisabled={!!matchingGroup || editMode}
              rules={{
                required: {
                  value: true,
                  message: t("required"),
                },
                onChange: (event) => {
                  handleAttributesGroupNameChange(event, event.target.value);
                },
              }}
            />
            {!!matchingGroup && (
              <input type="hidden" {...form.register("name")} />
            )}
            <FormGroup
              label={t("displayHeaderField")}
              labelIcon={
                <HelpItem
                  helpText={t("displayHeaderHintHelp")}
                  fieldLabelId="displayHeaderField"
                />
              }
              fieldId="kc-attributes-group-display-header"
            >
              <Grid hasGutter>
                <GridItem span={realm?.internationalizationEnabled ? 11 : 12}>
                  <TextInput
                    id="kc-attributes-group-display-header"
                    data-testid="attributes-group-display-header"
                    isDisabled={
                      (realm?.internationalizationEnabled &&
                        newAttributesGroupName !== "") ||
                      (editMode && attributesGroupDisplayPatternMatch)
                    }
                    value={
                      editMode
                        ? attributesGroupDisplayName
                        : realm?.internationalizationEnabled
                          ? generatedAttributesGroupDisplayName
                          : undefined
                    }
                    {...form.register("displayHeader")}
                  />
                  {generatedAttributesGroupDisplayName && (
                    <Alert
                      className="pf-v5-u-mt-sm"
                      variant="info"
                      isInline
                      isPlain
                      title={t("addAttributesGroupTranslationInfo")}
                    />
                  )}
                </GridItem>
                {realm?.internationalizationEnabled && (
                  <GridItem span={1}>
                    <Button
                      variant="link"
                      className="pf-m-plain"
                      data-testid="addAttributeDisplayNameTranslationBtn"
                      aria-label={t("addAttributeDisplayNameTranslation")}
                      isDisabled={!newAttributesGroupName && !editMode}
                      onClick={() => {
                        setType("displayHeader");
                        toggleModal();
                      }}
                      icon={<GlobeRouteIcon />}
                    />
                  </GridItem>
                )}
              </Grid>
            </FormGroup>
            <FormGroup
              label={t("displayDescriptionField")}
              labelIcon={
                <HelpItem
                  helpText={t("displayDescriptionHintHelp")}
                  fieldLabelId="displayDescriptionField"
                />
              }
              fieldId="kc-attributes-group-display-description"
            >
              <Grid hasGutter>
                <GridItem span={realm?.internationalizationEnabled ? 11 : 12}>
                  <TextInput
                    id="kc-attributes-group-display-description"
                    data-testid="attributes-group-display-description"
                    isDisabled={
                      (realm?.internationalizationEnabled &&
                        newAttributesGroupName !== "") ||
                      (editMode && attributesGroupDisplayPatternMatch)
                    }
                    value={
                      editMode
                        ? attributesGroupDisplayDescription
                        : realm?.internationalizationEnabled
                          ? generatedAttributesGroupDisplayDescription
                          : undefined
                    }
                    {...form.register("displayDescription")}
                  />
                  {generatedAttributesGroupDisplayDescription && (
                    <Alert
                      className="pf-v5-u-mt-sm"
                      variant="info"
                      isInline
                      isPlain
                      title={t("addAttributesGroupTranslationInfo")}
                    />
                  )}
                </GridItem>
                {realm?.internationalizationEnabled && (
                  <GridItem span={1}>
                    <Button
                      variant="link"
                      className="pf-m-plain"
                      data-testid="addAttributeDisplayDescriptionTranslationBtn"
                      aria-label={t(
                        "addAttributeDisplayDescriptionTranslation",
                      )}
                      isDisabled={!newAttributesGroupName && !editMode}
                      onClick={() => {
                        setType("displayDescription");
                        toggleModal();
                      }}
                      icon={<GlobeRouteIcon />}
                    />
                  </GridItem>
                )}
              </Grid>
            </FormGroup>
            <TextContent>
              <Text component="h2">{t("annotationsText")}</Text>
            </TextContent>
            <FormGroup label={t("annotationsText")} fieldId="kc-annotations">
              <KeyValueInput label={t("annotationsText")} name="annotations" />
            </FormGroup>
            <ActionGroup>
              <Button
                variant="primary"
                type="submit"
                data-testid="saveGroupBtn"
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                component={(props) => (
                  <Link
                    {...props}
                    to={toUserProfile({
                      realm: realmName,
                      tab: "attributes-group",
                    })}
                  />
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormProvider>
        </FormAccess>
      </PageSection>
    </>
  );
}
