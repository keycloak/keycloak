import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { useState, type JSX } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../../admin-client";
import { useConfirmDialog } from "../../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../../components/form/FormAccess";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
import { useParams } from "../../../utils/useParams";
import { toAuthorizationTab } from "../../routes/AuthenticationTab";
import {
  PolicyDetailsParams,
  toPolicyDetails,
} from "../../routes/PolicyDetails";
import { useIsAdminPermissionsClient } from "../../../utils/useIsAdminPermissionsClient";
import { toPermissionsConfigurationTabs } from "../../../permissions-configuration/routes/PermissionsConfigurationTabs";
import { NewPermissionPolicyDetailsParams } from "../../../permissions-configuration/routes/NewPermissionPolicy";
import { toPermissionPolicyDetails } from "../../../permissions-configuration/routes/PermissionPolicyDetails";
import { Aggregate } from "./Aggregate";
import { Client } from "./Client";
import { ClientScope, RequiredIdValue } from "./ClientScope";
import { Group, GroupValue } from "./Group";
import { JavaScript } from "./JavaScript";
import { LogicSelector } from "./LogicSelector";
import { NameDescription } from "./NameDescription";
import { Regex } from "./Regex";
import { Role } from "./Role";
import { Time } from "./Time";
import { User } from "./User";

import "./policy-details.css";

type Policy = Omit<PolicyRepresentation, "roles"> & {
  groups?: GroupValue[];
  clientScopes?: RequiredIdValue[];
  roles?: RequiredIdValue[];
};

const COMPONENTS: {
  [index: string]: () => JSX.Element;
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
} as const;

export const isValidComponentType = (value: string) => value in COMPONENTS;

export default function PolicyDetails() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { id, realm, policyId, policyType } = useParams<PolicyDetailsParams>();
  const { permissionClientId } = useParams<NewPermissionPolicyDetailsParams>();
  const navigate = useNavigate();
  const form = useForm();
  const { reset, handleSubmit } = form;
  const { addAlert, addError } = useAlerts();
  const [policy, setPolicy] = useState<PolicyRepresentation>();
  const isDisabled = policyType === "js";
  const isAdminPermissionsClient =
    useIsAdminPermissionsClient(permissionClientId);

  useFetch(
    async () => {
      if (policyId) {
        const result = await Promise.all([
          adminClient.clients.findOnePolicyWithType({
            id: permissionClientId ?? id,
            type: policyType!,
            policyId,
          }) as PolicyRepresentation | undefined,
          adminClient.clients.getAssociatedPolicies({
            id: permissionClientId ?? id,
            permissionId: policyId,
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
      return {};
    },
    ({ policy, policies }) => {
      reset({ ...policy, policies });
      setPolicy(policy);
    },
    [permissionClientId, id, policyType, policyId],
  );

  const onSubmitPolicy = async (policy: Policy) => {
    policy.groups = policy.groups?.filter((g) => g.id);
    policy.clientScopes = policy.clientScopes?.filter((c) => c.id);
    policy.roles = policy.roles
      ?.filter((r) => r.id)
      .map((r) => ({ ...r, required: r.required || false }));

    const clientId = isAdminPermissionsClient ? permissionClientId : id;
    const navigateTo = isAdminPermissionsClient
      ? toPermissionPolicyDetails
      : toPolicyDetails;

    try {
      if (policyId) {
        await adminClient.clients.updatePolicy(
          { id: clientId!, type: policyType!, policyId },
          policy,
        );
      } else {
        const result = await adminClient.clients.createPolicy(
          { id: clientId!, type: policyType! },
          policy,
        );

        navigate(
          navigateTo({
            realm: realm!,
            id: clientId!,
            permissionClientId: clientId!,
            policyId: result.id!,
            policyType: result.type!,
          }),
        );
      }
      addAlert(
        t((policyId ? "update" : "create") + "PolicySuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("policySaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deletePolicy",
    messageKey: "deletePolicyConfirm",
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPolicy({
          id: isAdminPermissionsClient ? permissionClientId : id,
          policyId,
        });
        addAlert(t("policyDeletedSuccess"), AlertVariant.success);
        navigate(
          isAdminPermissionsClient
            ? toPermissionsConfigurationTabs({
                realm: realm!,
                permissionClientId,
                tab: "policies",
              })
            : toAuthorizationTab({ realm, clientId: id, tab: "policies" }),
        );
      } catch (error) {
        addError("policyDeletedError", error);
      }
    },
  });

  if (policyId && !policy) {
    return <KeycloakSpinner />;
  }

  function getComponentType() {
    return isValidComponentType(policyType)
      ? COMPONENTS[policyType]
      : COMPONENTS["js"];
  }

  const ComponentType = getComponentType();

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={
          policyId ? policy?.name! : t("createPolicyOfType", { policyType })
        }
        dropdownItems={
          policyId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-policy"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit((policy) => onSubmitPolicy(policy))}
          role="anyone" // if you get this far it means you have access
        >
          <FormProvider {...form}>
            <NameDescription isDisabled={isDisabled} />
            <ComponentType />
            <LogicSelector isDisabled={isDisabled} />
          </FormProvider>
          <ActionGroup>
            <div className="pf-v5-u-mt-md">
              <Button
                isDisabled={isDisabled}
                variant={ButtonVariant.primary}
                className="pf-v5-u-mr-md"
                type="submit"
                data-testid="save"
              >
                {t("save")}
              </Button>

              <Button
                variant="link"
                data-testid="cancel"
                component={(props) => (
                  <Link
                    {...props}
                    to={
                      isAdminPermissionsClient
                        ? toPermissionsConfigurationTabs({
                            realm: realm!,
                            permissionClientId,
                            tab: "policies",
                          })
                        : toAuthorizationTab({
                            realm,
                            clientId: id,
                            tab: "policies",
                          })
                    }
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
