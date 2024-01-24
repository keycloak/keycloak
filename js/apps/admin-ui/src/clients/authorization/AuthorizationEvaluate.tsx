import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import type PolicyEvaluationResponse from "@keycloak/keycloak-admin-client/lib/defs/policyEvaluationResponse";
import type ResourceEvaluation from "@keycloak/keycloak-admin-client/lib/defs/resourceEvaluation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  ActionGroup,
  Button,
  ExpandableSection,
  FormGroup,
  PageSection,
  Panel,
  PanelHeader,
  PanelMainBody,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  Title,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { ForbiddenSection } from "../../ForbiddenSection";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { ClientSelect } from "../../components/client/ClientSelect";
import { FormAccess } from "../../components/form/FormAccess";
import {
  KeyValueType,
  keyValueToArray,
} from "../../components/key-value-form/key-value-convert";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { UserSelect } from "../../components/users/UserSelect";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";
import { FormFields } from "../ClientDetails";
import { defaultContextAttributes } from "../utils";
import { KeyBasedAttributeInput } from "./KeyBasedAttributeInput";
import { Results } from "./evaluate/Results";

interface EvaluateFormInputs
  extends Omit<ResourceEvaluation, "context" | "resources"> {
  alias: string;
  authScopes: string[];
  context: {
    attributes: Record<string, string>[];
  };
  resources?: Record<string, string>[];
  client: FormFields;
  user: string[];
}

export type AttributeType = {
  key: string;
  name: string;
  custom?: boolean;
  values?: {
    [key: string]: string;
  }[];
};

type ClientSettingsProps = {
  client: ClientRepresentation;
  save: () => void;
};

export type AttributeForm = Omit<
  EvaluateFormInputs,
  "context" | "resources"
> & {
  context: {
    attributes?: KeyValueType[];
  };
  resources?: KeyValueType[];
};

type Props = ClientSettingsProps & EvaluationResultRepresentation;

export const AuthorizationEvaluate = (props: Props) => {
  const { hasAccess } = useAccess();

  if (!hasAccess("view-users")) {
    return <ForbiddenSection permissionNeeded="view-users" />;
  }

  return <AuthorizationEvaluateContent {...props} />;
};

const AuthorizationEvaluateContent = ({ client }: Props) => {
  const form = useForm<EvaluateFormInputs>({ mode: "onChange" });
  const {
    control,
    register,
    reset,
    trigger,
    formState: { isValid, errors },
  } = form;
  const { t } = useTranslation();
  const { addError } = useAlerts();
  const realm = useRealm();

  const [scopesDropdownOpen, setScopesDropdownOpen] = useState(false);

  const [roleDropdownOpen, setRoleDropdownOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [applyToResourceType, setApplyToResourceType] = useState(false);
  const [resources, setResources] = useState<ResourceRepresentation[]>([]);
  const [scopes, setScopes] = useState<ScopeRepresentation[]>([]);
  const [evaluateResult, setEvaluateResult] =
    useState<PolicyEvaluationResponse>();

  const [clientRoles, setClientRoles] = useState<RoleRepresentation[]>([]);

  useFetch(
    () => adminClient.roles.find(),
    (roles) => {
      setClientRoles(roles);
    },
    [],
  );

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.listResources({
          id: client.id!,
        }),
        adminClient.clients.listAllScopes({
          id: client.id!,
        }),
      ]),
    ([resources, scopes]) => {
      setResources(resources);
      setScopes(scopes);
    },
    [],
  );

  const evaluate = async () => {
    if (!(await trigger())) {
      return;
    }
    const formValues = form.getValues();
    const keys = keyValueToArray(formValues.resources as KeyValueType[]);
    const resEval: ResourceEvaluation = {
      roleIds: formValues.roleIds ?? [],
      clientId: formValues.client.id!,
      userId: formValues.user![0],
      resources: resources
        .filter((resource) => Object.keys(keys).includes(resource.name!))
        .map((r) => ({
          ...r,
          scopes: r.scopes?.filter((s) =>
            Object.values(keys)
              .flatMap((v) => v)
              .includes(s.name!),
          ),
        })),
      entitlements: false,
      context: {
        attributes: Object.fromEntries(
          formValues.context.attributes
            .filter((item) => item.key || item.value !== "")
            .map(({ key, value }) => [key, value]),
        ),
      },
    };

    try {
      const evaluation = await adminClient.clients.evaluateResource(
        { id: client.id!, realm: realm.realm },
        resEval,
      );

      setEvaluateResult(evaluation);
    } catch (error) {
      addError("evaluateError", error);
    }
  };

  const user = useWatch({ control, name: "user", defaultValue: [] });
  const roles = useWatch({ control, name: "roleIds", defaultValue: [] });

  if (evaluateResult) {
    return (
      <Results
        evaluateResult={evaluateResult}
        refresh={evaluate}
        back={() => setEvaluateResult(undefined)}
      />
    );
  }

  return (
    <PageSection>
      <FormProvider {...form}>
        <Panel>
          <PanelHeader>
            <Title headingLevel="h2">{t("identityInformation")}</Title>
          </PanelHeader>
          <PanelMainBody>
            <FormAccess isHorizontal role="view-clients">
              <ClientSelect
                name="client"
                label="client"
                helpText={"clientHelp"}
                defaultValue={client.clientId}
              />
              <UserSelect
                name="user"
                label="users"
                helpText={t("selectUser")}
                defaultValue={[]}
                variant={SelectVariant.typeahead}
                isRequired={roles?.length === 0}
              />
              <FormGroup
                label={t("roles")}
                labelIcon={
                  <HelpItem helpText={t("rolesHelp")} fieldLabelId="roles" />
                }
                fieldId="realmRole"
                validated={errors.roleIds ? "error" : "default"}
                helperTextInvalid={t("required")}
                isRequired={user.length === 0}
              >
                <Controller
                  name="roleIds"
                  control={control}
                  defaultValue={[]}
                  rules={{
                    validate: (value) =>
                      (value || "").length > 0 || user.length > 0,
                  }}
                  render={({ field }) => (
                    <Select
                      placeholderText={t("selectARole")}
                      variant={SelectVariant.typeaheadMulti}
                      toggleId="role"
                      onToggle={setRoleDropdownOpen}
                      selections={field.value}
                      onSelect={(_, v) => {
                        const option = v.toString();
                        if (field.value?.includes(option)) {
                          field.onChange(
                            field.value.filter(
                              (item: string) => item !== option,
                            ),
                          );
                        } else {
                          field.onChange([...(field.value || []), option]);
                        }
                        setRoleDropdownOpen(false);
                      }}
                      onClear={(event) => {
                        event.stopPropagation();
                        field.onChange([]);
                      }}
                      aria-label={t("realmRole")}
                      isOpen={roleDropdownOpen}
                    >
                      {clientRoles.map((role) => (
                        <SelectOption
                          selected={role.name === field.value}
                          key={role.name}
                          value={role.name}
                        />
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
            </FormAccess>
          </PanelMainBody>
        </Panel>
        <Panel>
          <PanelHeader>
            <Title headingLevel="h2">{t("identityInformation")}</Title>
          </PanelHeader>
          <PanelMainBody>
            <FormAccess isHorizontal role="view-clients">
              <FormGroup
                label={t("applyToResourceType")}
                fieldId="applyToResourceType"
                labelIcon={
                  <HelpItem
                    helpText={t("applyToResourceTypeHelp")}
                    fieldLabelId="applyToResourceType"
                  />
                }
              >
                <Switch
                  id="applyToResource-switch"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={applyToResourceType}
                  onChange={setApplyToResourceType}
                  aria-label={t("applyToResourceType")}
                />
              </FormGroup>

              {!applyToResourceType ? (
                <FormGroup
                  label={t("resourcesAndScopes")}
                  id="resourcesAndScopes"
                  labelIcon={
                    <HelpItem
                      helpText={t("contextualAttributesHelp")}
                      fieldLabelId={`resourcesAndScopes`}
                    />
                  }
                  fieldId="resourcesAndScopes"
                >
                  <KeyBasedAttributeInput
                    selectableValues={resources.map<AttributeType>((item) => ({
                      name: item.name!,
                      key: item._id!,
                    }))}
                    resources={resources}
                    name="resources"
                  />
                </FormGroup>
              ) : (
                <>
                  <FormGroup
                    label={t("resourceType")}
                    isRequired
                    labelIcon={
                      <HelpItem
                        helpText={t("resourceTypeHelp")}
                        fieldLabelId="resourceType"
                      />
                    }
                    fieldId="client"
                    validated={errors.alias ? "error" : "default"}
                    helperTextInvalid={t("required")}
                  >
                    <KeycloakTextInput
                      id="alias"
                      aria-label="resource-type"
                      data-testid="alias"
                      {...register("alias", { required: true })}
                    />
                  </FormGroup>
                  <FormGroup
                    label={t("authScopes")}
                    labelIcon={
                      <HelpItem
                        helpText={t("scopesSelect")}
                        fieldLabelId="client"
                      />
                    }
                    fieldId="authScopes"
                  >
                    <Controller
                      name="authScopes"
                      defaultValue={[]}
                      control={control}
                      render={({ field }) => (
                        <Select
                          toggleId="authScopes"
                          onToggle={setScopesDropdownOpen}
                          onSelect={(_, v) => {
                            const option = v.toString();
                            if (field.value.includes(option)) {
                              field.onChange(
                                field.value.filter(
                                  (item: string) => item !== option,
                                ),
                              );
                            } else {
                              field.onChange([...field.value, option]);
                            }
                            setScopesDropdownOpen(false);
                          }}
                          selections={field.value}
                          variant={SelectVariant.typeaheadMulti}
                          typeAheadAriaLabel={t("selectAuthScopes")}
                          isOpen={scopesDropdownOpen}
                          aria-label={t("selectAuthScopes")}
                        >
                          {scopes.map((scope) => (
                            <SelectOption
                              selected={field.value.includes(scope.name!)}
                              key={scope.id}
                              value={scope.name}
                            />
                          ))}
                        </Select>
                      )}
                    />
                  </FormGroup>
                </>
              )}
              <ExpandableSection
                toggleText={t("contextualInfo")}
                onToggle={() => setIsExpanded(!isExpanded)}
                isExpanded={isExpanded}
              >
                <FormGroup
                  label={t("contextualAttributes")}
                  id="contextualAttributes"
                  labelIcon={
                    <HelpItem
                      helpText={t("contextualAttributesHelp")}
                      fieldLabelId={`contextualAttributes`}
                    />
                  }
                  helperTextInvalid={t("required")}
                  fieldId="contextualAttributes"
                >
                  <KeyBasedAttributeInput
                    selectableValues={defaultContextAttributes}
                    name="context.attributes"
                  />
                </FormGroup>
              </ExpandableSection>
            </FormAccess>
          </PanelMainBody>
        </Panel>
        <ActionGroup>
          <Button
            data-testid="authorization-eval"
            id="authorization-eval"
            className="pf-u-mr-md"
            isDisabled={!isValid}
            onClick={() => evaluate()}
          >
            {t("evaluate")}
          </Button>
          <Button
            data-testid="authorization-revert"
            id="authorization-revert"
            className="pf-u-mr-md"
            variant="link"
            onClick={() => reset()}
          >
            {t("revert")}
          </Button>
        </ActionGroup>
      </FormProvider>
    </PageSection>
  );
};
