import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { useEffect, useMemo } from "react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import type { EditAttributesGroupParams } from "../routes/EditAttributesGroup";
import { toUserProfile } from "../routes/UserProfile";
import { useUserProfile } from "./UserProfileContext";

import "../realm-settings-section.css";

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
      .map((annotation) => [annotation.key, annotation.value] as const)
  );
}

type FormFields = Required<Omit<UserProfileGroup, "annotations">> & {
  annotations: KeyValueType[];
};

const defaultValues: FormFields = {
  annotations: [{ key: "", value: "" }],
  displayDescription: "",
  displayHeader: "",
  name: "",
};

export default function AttributesGroupForm() {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { config, save } = useUserProfile();
  const navigate = useNavigate();
  const params = useParams<EditAttributesGroupParams>();
  const form = useForm<FormFields>({ defaultValues });

  const matchingGroup = useMemo(
    () => config?.groups?.find(({ name }) => name === params.name),
    [config?.groups]
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

    const success = await save({ ...config, groups });

    if (success) {
      navigate(toUserProfile({ realm, tab: "attributes-group" }));
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={
          matchingGroup
            ? "realm-settings:editGroupText"
            : "realm-settings:createGroupText"
        }
        divider
      />
      <PageSection variant="light" onSubmit={form.handleSubmit(onSubmit)}>
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("realm-settings:nameField")}
            fieldId="kc-name"
            isRequired
            helperTextInvalid={t("common:required")}
            validated={form.formState.errors.name ? "error" : "default"}
            labelIcon={
              <HelpItem
                helpText={t("realm-settings:nameHint")}
                fieldLabelId="realm-settings:nameField"
              />
            }
          >
            <KeycloakTextInput
              id="kc-name"
              isReadOnly={!!matchingGroup}
              {...form.register("name", { required: true })}
            />
            {!!matchingGroup && (
              <input type="hidden" {...form.register("name")} />
            )}
          </FormGroup>
          <FormGroup
            label={t("realm-settings:displayHeaderField")}
            fieldId="kc-display-header"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings:displayHeaderHint")}
                fieldLabelId="realm-settings:displayHeaderField"
              />
            }
          >
            <KeycloakTextInput
              id="kc-display-header"
              {...form.register("displayHeader")}
            />
          </FormGroup>
          <FormGroup
            label={t("realm-settings:displayDescriptionField")}
            fieldId="kc-display-description"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings:displayDescriptionHint")}
                fieldLabelId="realm-settings:displayDescriptionField"
              />
            }
          >
            <KeycloakTextInput
              id="kc-display-description"
              {...form.register("displayDescription")}
            />
          </FormGroup>
          <TextContent>
            <Text component="h2">{t("realm-settings:annotationsText")}</Text>
          </TextContent>
          <FormGroup
            label={t("realm-settings:annotationsText")}
            fieldId="kc-annotations"
          >
            <FormProvider {...form}>
              <KeyValueInput name="annotations" />
            </FormProvider>
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" type="submit">
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={toUserProfile({ realm, tab: "attributes-group" })}
                />
              )}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
