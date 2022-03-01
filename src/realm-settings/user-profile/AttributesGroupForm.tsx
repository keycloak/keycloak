import type { UserProfileGroup } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import {
  ActionGroup,
  ActionList,
  ActionListItem,
  Button,
  Flex,
  FlexItem,
  FormGroup,
  PageSection,
  Text,
  TextContent,
  TextInput,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import React, { useEffect, useMemo } from "react";
import {
  ArrayField,
  SubmitHandler,
  useFieldArray,
  useForm,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useHistory, useParams } from "react-router-dom";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import "../realm-settings-section.css";
import type { EditAttributesGroupParams } from "../routes/EditAttributesGroup";
import { toUserProfile } from "../routes/UserProfile";
import { useUserProfile } from "./UserProfileContext";

enum AnnotationType {
  String = "string",
  Unknown = "unknown",
}

type StringAnnotation = {
  type: AnnotationType.String;
  key: string;
  value: string;
};

type UnknownAnnotation = {
  type: AnnotationType.Unknown;
  key: string;
  value: unknown;
};

type Annotation = StringAnnotation | UnknownAnnotation;

function parseAnnotations(input: Record<string, unknown>) {
  return Object.entries(input).map<Annotation>(([key, value]) => {
    if (typeof value === "string") {
      return { type: AnnotationType.String, key, value };
    }

    return { type: AnnotationType.Unknown, key, value };
  });
}

function transformAnnotations(input: Annotation[]): Record<string, unknown> {
  return Object.fromEntries(
    input
      .filter((annotation) => annotation.key.length > 0)
      .map((annotation) => [annotation.key, annotation.value] as const)
  );
}

type FormFields = Required<Omit<UserProfileGroup, "annotations">> & {
  annotations: Annotation[];
};

const defaultValues: FormFields = {
  annotations: [{ type: AnnotationType.String, key: "", value: "" }],
  displayDescription: "",
  displayHeader: "",
  name: "",
};

export default function AttributesGroupForm() {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { config, save } = useUserProfile();
  const history = useHistory();
  const params = useParams<Partial<EditAttributesGroupParams>>();
  const form = useForm<FormFields>({ defaultValues, shouldUnregister: false });
  const annotationsField = useFieldArray<Annotation>({
    control: form.control,
    name: "annotations",
  });

  const annotations = useWatch({
    control: form.control,
    name: "annotations",
    defaultValue: defaultValues.annotations,
  });

  const annotationsValid = annotations
    .filter(
      (annotation): annotation is StringAnnotation =>
        annotation.type === AnnotationType.String
    )
    .every(
      ({ key, value }) => key.trim().length !== 0 && value.trim().length !== 0
    );

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

    if (annotations.length === 0) {
      annotations.push({ type: AnnotationType.String, key: "", value: "" });
    }

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
      history.push(toUserProfile({ realm, tab: "attributesGroup" }));
    }
  };

  function addAnnotation() {
    annotationsField.append({
      type: AnnotationType.String,
      key: "",
      value: "",
    });
  }

  function removeAnnotation(index: number) {
    annotationsField.remove(index);
  }

  return (
    <>
      <ViewHeader
        titleKey={
          matchingGroup
            ? "attributes-group:editGroupText"
            : "attributes-group:createGroupText"
        }
        divider
      />
      <PageSection variant="light" onSubmit={form.handleSubmit(onSubmit)}>
        <FormAccess isHorizontal role="manage-realm">
          <FormGroup
            label={t("attributes-group:nameField")}
            fieldId="kc-name"
            isRequired
            helperTextInvalid={t("common:required")}
            validated={form.errors.name ? "error" : "default"}
            labelIcon={
              <HelpItem
                helpText="attributes-group:nameHint"
                fieldLabelId="attributes-group:nameField"
              />
            }
          >
            <TextInput
              ref={form.register({ required: true })}
              type="text"
              id="kc-name"
              name="name"
              isDisabled={!!matchingGroup}
            />
          </FormGroup>
          <FormGroup
            label={t("attributes-group:displayHeaderField")}
            fieldId="kc-display-header"
            labelIcon={
              <HelpItem
                helpText="attributes-group:displayHeaderHint"
                fieldLabelId="attributes-group:displayHeaderField"
              />
            }
          >
            <TextInput
              ref={form.register()}
              type="text"
              id="kc-display-header"
              name="displayHeader"
            />
          </FormGroup>
          <FormGroup
            label={t("attributes-group:displayDescriptionField")}
            fieldId="kc-display-description"
            labelIcon={
              <HelpItem
                helpText="attributes-group:displayDescriptionHint"
                fieldLabelId="attributes-group:displayDescriptionField"
              />
            }
          >
            <TextInput
              ref={form.register()}
              type="text"
              id="kc-display-description"
              name="displayDescription"
            />
          </FormGroup>
          <TextContent>
            <Text component="h2">{t("attributes-group:annotationsText")}</Text>
          </TextContent>
          <FormGroup
            label={t("attributes-group:annotationsText")}
            fieldId="kc-annotations"
          >
            <Flex direction={{ default: "column" }}>
              {annotationsField.fields
                .filter(
                  (
                    annotation
                  ): annotation is Partial<
                    ArrayField<StringAnnotation, "id">
                  > => annotation.type === AnnotationType.String
                )
                .map((item, index) => (
                  <Flex key={item.id}>
                    <FlexItem grow={{ default: "grow" }}>
                      <TextInput
                        name={`annotations[${index}].key`}
                        ref={form.register()}
                        placeholder={t("attributes-group:keyPlaceholder")}
                        aria-label={t("attributes-group:keyLabel")}
                        defaultValue={item.key}
                      />
                    </FlexItem>
                    <FlexItem
                      grow={{ default: "grow" }}
                      spacer={{ default: "spacerNone" }}
                    >
                      <TextInput
                        name={`annotations[${index}].value`}
                        ref={form.register()}
                        placeholder={t("attributes-group:valuePlaceholder")}
                        aria-label={t("attributes-group:valueLabel")}
                        defaultValue={item.value}
                      />
                    </FlexItem>
                    <FlexItem>
                      <Button
                        variant="link"
                        title={t("attributes-group:removeAnnotationText")}
                        aria-label={t("attributes-group:removeAnnotationText")}
                        isDisabled={annotationsField.fields.length === 1}
                        onClick={() => removeAnnotation(index)}
                      >
                        <MinusCircleIcon />
                      </Button>
                    </FlexItem>
                  </Flex>
                ))}
            </Flex>
            <ActionList>
              <ActionListItem>
                <Button
                  className="pf-u-px-0 pf-u-mt-sm"
                  variant="link"
                  icon={<PlusCircleIcon />}
                  isDisabled={!annotationsValid}
                  onClick={addAnnotation}
                >
                  {t("attributes-group:addAnnotationText")}
                </Button>
              </ActionListItem>
            </ActionList>
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
                  to={toUserProfile({ realm, tab: "attributesGroup" })}
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
