import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import type PolicyEvaluationResponse from "@keycloak/keycloak-admin-client/lib/defs/policyEvaluationResponse";
import type ResourceEvaluation from "@keycloak/keycloak-admin-client/lib/defs/resourceEvaluation";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import {
  HelpItem,
  SelectControl,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  ExpandableSection,
  FormGroup,
  PageSection,
  Panel,
  PanelHeader,
  PanelMainBody,
  Switch,
  Title,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { ForbiddenSection } from "../../ForbiddenSection";
import { useAdminClient } from "../../admin-client";
import { ClientSelect } from "../../components/client/ClientSelect";
import { FormAccess } from "../../components/form/FormAccess";
import {
  KeyValueType,
  keyValueToArray,
} from "../../components/key-value-form/key-value-convert";
import { UserSelect } from "../../components/users/UserSelect";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
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
  const { adminClient } = useAdminClient();

  const form = useForm<EvaluateFormInputs>({ mode: "onChange" });
  const {
    reset,
    trigger,
    formState: { isValid },
  } = form;
  const { t } = useTranslation();
  const { addError } = useAlerts();
  const realm = useRealm();
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
                variant="typeahead"
                isRequired
              />
              <SelectControl
                name="roleIds"
                label={t("roles")}
                labelIcon={t("rolesHelp")}
                variant="typeaheadMulti"
                placeholderText={t("selectARole")}
                controller={{
                  defaultValue: [],
                  rules: {
                    required: true,
                  },
                }}
                options={clientRoles.map((role) => role.name!)}
              />
            </FormAccess>
          </PanelMainBody>
        </Panel>
        <Panel>
          <PanelHeader>
            <Title headingLevel="h2">{t("permissions")}</Title>
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
                  onChange={(_event, val) => setApplyToResourceType(val)}
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
                  <TextControl
                    name="alias"
                    label={t("resourceType")}
                    labelIcon={t("resourceTypeHelp")}
                    rules={{ required: t("required") }}
                  />
                  <SelectControl
                    name="authScopes"
                    label={t("authScopes")}
                    labelIcon={t("scopesSelect")}
                    controller={{
                      defaultValue: [],
                    }}
                    variant="typeaheadMulti"
                    options={scopes.map((s) => s.name!)}
                  />
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
            className="pf-v5-u-mr-md"
            isDisabled={!isValid}
            onClick={() => evaluate()}
          >
            {t("evaluate")}
          </Button>
          <Button
            data-testid="authorization-revert"
            id="authorization-revert"
            className="pf-v5-u-mr-md"
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
