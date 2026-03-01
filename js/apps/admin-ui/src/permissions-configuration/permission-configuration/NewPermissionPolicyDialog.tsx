import PolicyRepresentation, {
  DecisionStrategy,
  Logic,
} from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import { useTranslation } from "react-i18next";
import {
  Modal,
  ModalVariant,
  TextContent,
  Text,
  TextVariants,
  ActionGroup,
  Button,
  Form,
  ButtonVariant,
  AlertVariant,
} from "@patternfly/react-core";
import {
  SelectControl,
  TextControl,
  useAlerts,
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
import { Aggregate } from "../../clients/authorization/policy/Aggregate";
import { capitalize } from "lodash-es";
import { useEffect, type JSX } from "react";

type Policy = Omit<PolicyRepresentation, "roles"> & {
  groups?: GroupValue[];
  clientScopes?: RequiredIdValue[];
  roles?: RequiredIdValue[];
  clients?: [];
};

type ComponentsProps = {
  isPermissionClient?: boolean;
  permissionClientId: string;
};

const defaultValues: Policy = {
  name: "",
  description: "",
  type: "group",
  policies: [],
  decisionStrategy: DecisionStrategy.UNANIMOUS,
  logic: Logic.POSITIVE,
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
  default: Group,
} as const;

export const isValidComponentType = (value: string) => value in COMPONENTS;

type NewPermissionConfigurationDialogProps = {
  permissionClientId: string;
  providers: PolicyProviderRepresentation[];
  policies: PolicyRepresentation[];
  resourceType: string;
  toggleDialog: () => void;
  onAssign: (newPolicy: PolicyRepresentation) => void;
};

export const NewPermissionPolicyDialog = ({
  permissionClientId,
  providers,
  policies,
  toggleDialog,
  onAssign,
}: NewPermissionConfigurationDialogProps) => {
  const { adminClient } = useAdminClient();
  const { realmRepresentation } = useRealm();
  const { t } = useTranslation();
  const form = useForm<Policy>({
    mode: "onChange",
    defaultValues,
  });
  const { addAlert, addError } = useAlerts();
  const { handleSubmit, reset } = form;
  const isPermissionClient = realmRepresentation?.adminPermissionsEnabled;

  const policyTypeSelector = useWatch({
    control: form.control,
    name: "type",
  });

  function getComponentType() {
    if (policyTypeSelector && isValidComponentType(policyTypeSelector)) {
      return COMPONENTS[policyTypeSelector];
    }
    return COMPONENTS["default"];
  }

  const ComponentType = getComponentType();

  useEffect(() => {
    if (policyTypeSelector) {
      const { name, description, decisionStrategy, logic } = form.getValues();

      reset({
        type: policyTypeSelector,
        name,
        description,
        decisionStrategy,
        logic,
      });
    }
  }, [policyTypeSelector, reset, form]);

  const save = async (policy: Policy) => {
    const { groups, roles, policies, clients, ...rest } = policy;

    const cleanedPolicy = {
      ...rest,
      ...(groups && groups.length > 0 && { groups }),
      ...(roles && roles.length > 0 && { roles }),
      ...(policies && policies.length > 0 && { policies }),
      ...(clients && clients.length > 0 && { clients }),
      ...(rest.type === "group" &&
        (!groups || groups.length === 0) && { groups: [] }),
      ...(rest.type === "client" &&
        (!clients || clients.length === 0) && { clients: [] }),
    };

    try {
      const createdPolicy = await adminClient.clients.createPolicy(
        { id: permissionClientId, type: policyTypeSelector! },
        cleanedPolicy,
      );

      onAssign(createdPolicy);
      toggleDialog();
      addAlert(t("createPolicySuccess"), AlertVariant.success);
    } catch (error) {
      addError("policySaveError", error);
    }
  };

  return (
    <Modal
      aria-label={t("createPermissionPolicy")}
      variant={ModalVariant.medium}
      header={
        <TextContent>
          <Text component={TextVariants.h1}>{t("createPermissionPolicy")}</Text>
        </TextContent>
      }
      isOpen
      onClose={toggleDialog}
    >
      <Form
        id="createPermissionPolicy-form"
        onSubmit={async (e) => {
          e.stopPropagation();
          await handleSubmit(save)(e);
        }}
        isHorizontal
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
            permissionClientId={permissionClientId}
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
            <Button variant="link" data-testid="cancel" onClick={toggleDialog}>
              {t("cancel")}
            </Button>
          </div>
        </ActionGroup>
      </Form>
    </Modal>
  );
};
