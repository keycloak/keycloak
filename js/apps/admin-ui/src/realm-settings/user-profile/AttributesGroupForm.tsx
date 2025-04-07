import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import { HelpItem, TextControl, useAlerts } from "@keycloak/keycloak-ui-shared";
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
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import "../realm-settings-section.css";
import type { EditAttributesGroupParams } from "../routes/EditAttributesGroup";
import { toUserProfile } from "../routes/UserProfile";
import { useUserProfile } from "./UserProfileContext";
import { saveTranslations, Translations } from "./attribute/TranslatableField";
import { TranslatableField } from "./attribute/TranslatableField";

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
  translation: { key: [] },
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

  const onSubmit: SubmitHandler<FormFields> = async (values) => {
    if (!config) {
      return;
    }

    const groups = [...(config.groups ?? [])];
    const updateAt = matchingGroup ? groups.indexOf(matchingGroup) : -1;
    const { translation, ...groupValues } = values;
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

    if (success) {
      if (realm?.internationalizationEnabled) {
        try {
          await saveTranslations({
            adminClient,
            realmName,
            translationsData: { translation },
          });
        } catch (error) {
          addError(t("errorSavingTranslations"), error);
        }
      }
      navigate(toUserProfile({ realm: realmName, tab: "attributes-group" }));
    }
  };

  return (
    <FormProvider {...form}>
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
            }}
          />
          <FormGroup
            label={t("displayHeader")}
            labelIcon={
              <HelpItem
                helpText={t("displayHeaderHintHelp")}
                fieldLabelId="displayHeader"
              />
            }
            fieldId="kc-attributes-group-display-header"
          >
            <TranslatableField
              fieldName="displayHeader"
              attributeName="name"
              prefix="profile.attribute-group"
            />
          </FormGroup>
          <FormGroup
            label={t("displayDescription")}
            labelIcon={
              <HelpItem
                helpText={t("displayDescriptionHintHelp")}
                fieldLabelId="displayDescription"
              />
            }
            fieldId="kc-attributes-group-display-description"
          >
            <TranslatableField
              fieldName="displayDescription"
              attributeName="name"
              prefix="profile.attribute-group-description"
            />
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
