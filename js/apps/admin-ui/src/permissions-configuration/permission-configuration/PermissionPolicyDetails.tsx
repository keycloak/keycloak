import PolicyRepresentation, {
  DecisionStrategy,
  Logic,
} from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import { useTranslation } from "react-i18next";
import {
  ActionGroup,
  Button,
  ButtonVariant,
  AlertVariant,
  PageSection,
} from "@patternfly/react-core";
import {
  KeycloakSpinner,
  SelectControl,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { Client } from "../../clients/authorization/policy/Client";
import { User } from "../../clients/authorization/policy/User";
import {
  ClientScope,
  RequiredIdValue,
} from "../../clients/authorization/policy/ClientScope";
import { Group, GroupValue } from "../../clients/authorization/policy/Group";
import { Regex } from "../../clients/authorization/policy/Regex";
import { Role } from "../../clients/authorization/policy/Role";
import { Time } from "../../clients/authorization/policy/Time";
import { JavaScript } from "../../clients/authorization/policy/JavaScript";
import { LogicSelector } from "../../clients/authorization/policy/LogicSelector";
import { Aggregate } from "./permission-policy/Aggregate";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { Link, useParams } from "react-router-dom";
import { toPermissionsConfigurationTabs } from "../routes/PermissionsConfigurationTabs";
import { useState, type JSX } from "react";
import { capitalize, sortBy } from "lodash-es";
import { FormAccess } from "../../components/form/FormAccess";
import { PermissionPolicyDetailsParams } from "../routes/PermissionPolicyDetails";

type Policy = Omit<PolicyRepresentation, "roles"> & {
  groups?: GroupValue[];
  clientScopes?: RequiredIdValue[];
  roles?: RequiredIdValue[];
};

type ComponentsProps = {
  isPermissionClient?: boolean;
  permissionClientId: string;
};

const defaultValues: Policy = {
  name: "",
  description: "",
  type: "aggregate",
  policies: [],
  decisionStrategy: "UNANIMOUS" as DecisionStrategy,
  logic: "POSITIVE" as Logic,
};

const COMPONENTS: {
  [index: string]: ({
    isPermissionClient,
    permissionClientId,
  }: ComponentsProps) => JSX.Element;
} = {
  aggregate: Aggregate,
  client: Client,
  user: User,
  "client-scope": ClientScope,
  group: Group,
  regex: Regex,
  role: Role,
  time: Time,
  js: JavaScript,
  default: Aggregate,
} as const;

export const isValidComponentType = (value: string) => value in COMPONENTS;

export default function PermissionPolicyDetails() {
  const { adminClient } = useAdminClient();
  const { realmRepresentation } = useRealm();
  const { permissionClientId, policyId, realm, resourceType } =
    useParams<PermissionPolicyDetailsParams>();
  const { t } = useTranslation();
  const form = useForm<Policy>({
    mode: "onChange",
    defaultValues,
  });
  const { addAlert, addError } = useAlerts();
  const { reset, handleSubmit } = form;
  const [providers, setProviders] = useState<PolicyProviderRepresentation[]>();
  const [policies, setPolicies] = useState<PolicyRepresentation[]>();
  const [policy, setPolicy] = useState<PolicyRepresentation>();
  const isPermissionClient = realmRepresentation?.adminPermissionsEnabled;

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.listPolicyProviders({
          id: permissionClientId!,
        }),
        adminClient.clients.listPolicies({
          id: permissionClientId!,
          permission: "false",
        }),
      ]),
    ([providers, policies]) => {
      const filteredProviders = providers.filter(
        (p) => p.type !== "resource" && p.type !== "scope",
      );
      setProviders(
        sortBy(
          filteredProviders,
          (provider: PolicyProviderRepresentation) => provider.type,
        ),
      );
      setPolicies(policies || []);
    },
    [permissionClientId],
  );

  useFetch(
    async () => {
      if (policyId) {
        const result = await Promise.all([
          adminClient.clients.findOnePolicy({
            id: permissionClientId!,
            type: resourceType!,
            policyId,
          }) as PolicyRepresentation | undefined,
          adminClient.clients.getAssociatedPolicies({
            id: permissionClientId!,
            permissionId: policyId!,
          }),
        ]);

        if (!result[0]) {
          throw new Error(t("notFound"));
        }

        return {
          policy: result[0],
          policies: result[1].map((p) => p.id),
        };
      }
      return { policy: defaultValues, policies: defaultValues.policies };
    },
    ({ policy, policies }) => {
      reset({ ...defaultValues, ...policy, policies });
      setPolicy(policy);
    },
    [permissionClientId, resourceType, policyId],
  );

  const policyTypeSelector = useWatch({
    control: form.control,
    name: "type",
  });

  function getComponentType() {
    if (isValidComponentType(policyTypeSelector!)) {
      return COMPONENTS[policyTypeSelector!];
    }
    return COMPONENTS["default"];
  }

  const ComponentType = getComponentType();

  const save = async (policy: Policy) => {
    // remove entries that only have the boolean set and no id
    policy.groups = policy.groups?.filter((g) => g.id);
    policy.clientScopes = policy.clientScopes?.filter((c) => c.id);
    policy.roles = policy.roles
      ?.filter((r) => r.id)
      .map((r) => ({ ...r, required: r.required || false }));

    try {
      await adminClient.clients.createPolicy(
        { id: permissionClientId!, type: policyTypeSelector! },
        policy,
      );
      addAlert(t("create" + "PolicySuccess"), AlertVariant.success);
    } catch (error) {
      addError("policySaveError", error);
    }
  };

  if (policyId && !policy) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader titleKey={t("createAPolicy")} />
      <PageSection variant="light">
        <FormAccess
          id="createAPolicy-form"
          onSubmit={handleSubmit(save)}
          isHorizontal
          role="anyone"
        >
          <FormProvider {...form}>
            <TextControl
              name="name"
              label={t("name")}
              rules={{ required: t("required") }}
            />
            <TextControl name="description" label={t("description")} />
            {providers && providers.length > 0 && (
              <SelectControl
                name="type"
                label={t("policyType")}
                labelIcon={t("policyTypeHelpText")}
                options={providers.map((provider) => ({
                  key: provider.type!,
                  value: capitalize(provider.type!),
                }))}
                controller={{ defaultValue: "" }}
              />
            )}
            <ComponentType
              isPermissionClient={isPermissionClient}
              permissionClientId={permissionClientId!}
            />
            <LogicSelector />
          </FormProvider>
          <ActionGroup>
            <div className="pf-v5-u-mt-md">
              <Button
                variant={ButtonVariant.primary}
                className="pf-v5-u-mr-md"
                type="submit"
                data-testid="save"
                isDisabled={
                  policies?.length === 0 && policyTypeSelector === "aggregate"
                }
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                data-testid="cancel"
                component={(props) => (
                  <Link
                    {...props}
                    to={toPermissionsConfigurationTabs({
                      realm: realm!,
                      tab: "policies",
                    })}
                  />
                )}
              >
                {t("cancel")}
              </Button>
            </div>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
