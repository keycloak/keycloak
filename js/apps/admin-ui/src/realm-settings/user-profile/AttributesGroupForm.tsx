/* eslint-disable @typescript-eslint/no-empty-function */
import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { HelpItem, TextControl, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Alert,
  Button,
  FormGroup,
  FormHelperText,
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
import { FormAccess } from "../../components/form/FormAccess";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";
import "../realm-settings-section.css";
import type { EditAttributesGroupParams } from "../routes/EditAttributesGroup";
import { toUserProfile } from "../routes/UserProfile";
import { useUserProfile } from "./UserProfileContext";
import {
  AddTranslationsDialog,
  saveTranslations,
  Translations,
  TranslationsType,
} from "./attribute/AddTranslationsDialog";

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

type FormFields = Required<Omit<UserProfileGroup, "annotations">> &
  Translations & {
    annotations: KeyValueType[];
  };

const defaultValues: FormFields = {
  annotations: [],
  displayDescription: "",
  displayHeader: "",
  name: "",
  translations: [],
  key: "",
};

export default function AttributesGroupForm() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const { config, save } = useUserProfile();
  const navigate = useNavigate();
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

  const matchingGroup = useMemo(
    () => config?.groups?.find(({ name }) => name === params.name),
    [config?.groups, params.name],
  );

  useEffect(() => {
    if (!matchingGroup) {
      return;
    }

    const annotations = matchingGroup.annotations
      ? parseAnnotations(matchingGroup.annotations)
      : [];

    form.reset({ ...defaultValues, ...matchingGroup, annotations });
  }, [matchingGroup, form]);

  useEffect(() => {
    form.setValue(
      "displayHeader",
      matchingGroup?.displayHeader || generatedAttributesGroupDisplayName || "",
    );
    form.setValue(
      "displayDescription",
      matchingGroup?.displayDescription ||
        generatedAttributesGroupDisplayDescription ||
        "",
    );
  }, [
    generatedAttributesGroupDisplayName,
    generatedAttributesGroupDisplayDescription,
    matchingGroup,
    form,
  ]);

  const onSubmit: SubmitHandler<FormFields> = async (values) => {
    if (!config) {
      return;
    }

    const groups = [...(config.groups ?? [])];
    const updateAt = matchingGroup ? groups.indexOf(matchingGroup) : -1;
    const { translations, key, ...groupValues } = values;
    const updatedGroup: UserProfileGroup = {
      ...groupValues,
      annotations: transformAnnotations(values.annotations),
    };

    if (updateAt === -1) {
      groups.push(updatedGroup);
    } else {
      groups[updateAt] = updatedGroup;
    }

    const success = await save({ ...config, groups });

    if (success && realm?.internationalizationEnabled) {
      try {
        await saveTranslations({
          adminClient,
          realmName,
          translationsData: {
            key,
            translations,
          },
        });
      } catch (error) {
        addError(t("errorSavingTranslations"), error);
      }
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

  const groupDisplayNameKey =
    type === "displayHeader"
      ? formattedAttributesGroupDisplayName
      : `profile.attribute-group.${newAttributesGroupName}`;
  const groupDisplayDescriptionKey =
    type === "displayDescription"
      ? formattedAttributesGroupDisplayDescription
      : `profile.attribute-group-description.${newAttributesGroupName}`;

  return (
    <FormProvider {...form}>
      {addTranslationsModalOpen && (
        <AddTranslationsDialog
          translationKey={
            type === "displayHeader"
              ? groupDisplayNameKey
              : groupDisplayDescriptionKey
          }
          fieldName={type || "displayDescription"}
          toggleDialog={toggleModal}
        />
      )}
      <ViewHeader
        titleKey={matchingGroup ? "editGroupText" : "createGroupText"}
        divider
      />
      <PageSection variant="light" onSubmit={form.handleSubmit(onSubmit)}>
        <FormAccess isHorizontal role="manage-realm">
          <TextControl
            name="name"
            label={t("nameField")}
            labelIcon={t("nameHintHelp")}
            isDisabled={!!matchingGroup || editMode}
            rules={{
              required: t("required"),
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
                  <FormHelperText>
                    <Alert
                      variant="info"
                      isInline
                      isPlain
                      title={t("addTranslationsModalSubTitle", {
                        fieldName: t("displayHeader"),
                      })}
                    />
                  </FormHelperText>
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
                  <FormHelperText>
                    <Alert
                      variant="info"
                      isInline
                      isPlain
                      title={t("addTranslationsModalSubTitle", {
                        fieldName: t("displayDescription"),
                      })}
                    />
                  </FormHelperText>
                )}
              </GridItem>
              {realm?.internationalizationEnabled && (
                <GridItem span={1}>
                  <Button
                    variant="link"
                    className="pf-m-plain"
                    data-testid="addAttributeDisplayDescriptionTranslationBtn"
                    aria-label={t("addAttributeDisplayDescriptionTranslation")}
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
            <Button variant="primary" type="submit" data-testid="saveGroupBtn">
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
        </FormAccess>
      </PageSection>
    </FormProvider>
  );
}
