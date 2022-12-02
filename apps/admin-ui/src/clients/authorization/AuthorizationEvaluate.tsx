import {
  ActionGroup,
  Button,
  ExpandableSection,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import type PolicyEvaluationResponse from "@keycloak/keycloak-admin-client/lib/defs/policyEvaluationResponse";
import type ResourceEvaluation from "@keycloak/keycloak-admin-client/lib/defs/resourceEvaluation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";

import { ClientSelect } from "../../components/client/ClientSelect";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import type { KeyValueType } from "../../components/key-value-form/key-value-convert";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { FormPanel } from "../../components/scroll-form/FormPanel";
import { UserSelect } from "../../components/users/UserSelect";
import { useAccess } from "../../context/access/Access";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { ForbiddenSection } from "../../ForbiddenSection";
import { FormFields } from "../ClientDetails";
import { defaultContextAttributes } from "../utils";
import { Results } from "./evaluate/Results";
import { KeyBasedAttributeInput } from "./KeyBasedAttributeInput";

import "./auth-evaluate.css";

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

export const AuthorizationEvaluate = ({ client }: Props) => {
  const form = useForm<EvaluateFormInputs>({ mode: "onChange" });
  const {
    control,
    register,
    reset,
    trigger,
    formState: { isValid, errors },
  } = form;
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();
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

  const { hasAccess } = useAccess();
  if (!hasAccess("view-users"))
    return <ForbiddenSection permissionNeeded="view-users" />;

  useFetch(
    () => adminClient.roles.find(),
    (roles) => {
      setClientRoles(roles);
    },
    []
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
    []
  );

  const evaluate = async () => {
    if (!(await trigger())) {
      return;
    }
    const formValues = form.getValues();
    const keys = formValues.resources?.map(({ key }) => key);
    const resEval: ResourceEvaluation = {
      roleIds: formValues.roleIds ?? [],
      clientId: formValues.client.id!,
      userId: formValues.user[0],
      resources: formValues.resources?.filter((resource) =>
        keys?.includes(resource.name!)
      ),
      entitlements: false,
      context: {
        attributes: Object.fromEntries(
          formValues.context.attributes
            .filter((item) => item.key || item.value !== "")
            .map(({ key, value }) => [key, value])
        ),
      },
    };

    const evaluation = await adminClient.clients.evaluateResource(
      { id: client.id!, realm: realm.realm },
      resEval
    );

    setEvaluateResult(evaluation);
    return evaluation;
  };

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
        <FormPanel
          className="kc-identity-information"
          title={t("clients:identityInformation")}
        >
          <FormAccess isHorizontal role="view-clients">
            <ClientSelect
              name="client"
              label="client"
              namespace="clients"
              helpText={"clients-help:client"}
              defaultValue={client.clientId}
            />
            <UserSelect
              name="user"
              label="users"
              helpText="clients-help:selectUser"
              defaultValue=""
              variant={SelectVariant.typeahead}
              isRequired
            />
            <FormGroup
              label={t("roles")}
              labelIcon={
                <HelpItem
                  helpText="clients-help:roles"
                  fieldLabelId="clients:roles"
                />
              }
              fieldId="realmRole"
              validated={errors.roleIds ? "error" : "default"}
              helperTextInvalid={t("common:required")}
              isRequired
            >
              <Controller
                name="roleIds"
                control={control}
                defaultValue={[]}
                rules={{ validate: (value) => (value || "").length > 0 }}
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
                          field.value.filter((item: string) => item !== option)
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
        </FormPanel>
        <FormPanel className="kc-permissions" title={t("common:permissions")}>
          <FormAccess isHorizontal role="view-clients">
            <FormGroup
              label={t("applyToResourceType")}
              fieldId="applyToResourceType"
              labelIcon={
                <HelpItem
                  helpText="clients-help:applyToResourceType"
                  fieldLabelId="clients:applyToResourceType"
                />
              }
            >
              <Switch
                id="applyToResource-switch"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={applyToResourceType}
                onChange={setApplyToResourceType}
                aria-label={t("applyToResourceType")}
              />
            </FormGroup>

            {!applyToResourceType ? (
              <FormGroup
                label={t("resourcesAndAuthScopes")}
                id="resourcesAndAuthScopes"
                isRequired
                labelIcon={
                  <HelpItem
                    helpText={t("clients-help:contextualAttributes")}
                    fieldLabelId={`resourcesAndAuthScopes`}
                  />
                }
                helperTextInvalid={t("common:required")}
                fieldId="resourcesAndAuthScopes"
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
                      helpText="clients-help:resourceType"
                      fieldLabelId="clients:resourceType"
                    />
                  }
                  fieldId="client"
                  validated={errors.alias ? "error" : "default"}
                  helperTextInvalid={t("common:required")}
                >
                  <KeycloakTextInput
                    id="alias"
                    data-testid="alias"
                    {...register("alias", { required: true })}
                  />
                </FormGroup>
                <FormGroup
                  label={t("authScopes")}
                  labelIcon={
                    <HelpItem
                      helpText="clients-help:scopesSelect"
                      fieldLabelId="clients:client"
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
                                (item: string) => item !== option
                              )
                            );
                          } else {
                            field.onChange([...field.value, option]);
                          }
                          setScopesDropdownOpen(false);
                        }}
                        selections={field.value}
                        variant={SelectVariant.typeaheadMulti}
                        aria-label={t("authScopes")}
                        isOpen={scopesDropdownOpen}
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
                    helpText={t("clients-help:contextualAttributes")}
                    fieldLabelId={`contextualAttributes`}
                  />
                }
                helperTextInvalid={t("common:required")}
                fieldId="contextualAttributes"
              >
                <KeyBasedAttributeInput
                  selectableValues={defaultContextAttributes}
                  name="context.attributes"
                />
              </FormGroup>
            </ExpandableSection>
          </FormAccess>
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
              {t("common:revert")}
            </Button>
          </ActionGroup>
        </FormPanel>
      </FormProvider>
    </PageSection>
  );
};
