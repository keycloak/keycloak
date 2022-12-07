import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { FunctionComponent, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom-v5-compat";

import { useAlerts } from "../../../components/alert/Alerts";
import { useConfirmDialog } from "../../../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { KeycloakSpinner } from "../../../components/keycloak-spinner/KeycloakSpinner";
import { ViewHeader } from "../../../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";
import { useParams } from "../../../utils/useParams";
import { toAuthorizationTab } from "../../routes/AuthenticationTab";
import {
  PolicyDetailsParams,
  toPolicyDetails,
} from "../../routes/PolicyDetails";
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
  [index: string]: FunctionComponent;
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
  const { t } = useTranslation("clients");
  const { id, realm, policyId, policyType } = useParams<PolicyDetailsParams>();
  const navigate = useNavigate();
  const form = useForm({ shouldUnregister: false });
  const { reset, handleSubmit } = form;

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [policy, setPolicy] = useState<PolicyRepresentation>();

  useFetch(
    async () => {
      if (policyId) {
        const result = await Promise.all([
          adminClient.clients.findOnePolicy({
            id,
            type: policyType,
            policyId,
          }) as PolicyRepresentation | undefined,
          adminClient.clients.getAssociatedPolicies({
            id,
            permissionId: policyId,
          }),
        ]);

        if (!result[0]) {
          throw new Error(t("common:notFound"));
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
    []
  );

  const save = async (policy: Policy) => {
    // remove entries that only have the boolean set and no id
    policy.groups = policy.groups?.filter((g) => g.id);
    policy.clientScopes = policy.clientScopes?.filter((c) => c.id);
    policy.roles = policy.roles
      ?.filter((r) => r.id)
      .map((r) => ({ ...r, required: r.required || false }));

    try {
      if (policyId) {
        await adminClient.clients.updatePolicy(
          { id, type: policyType, policyId },
          policy
        );
      } else {
        const result = await adminClient.clients.createPolicy(
          { id, type: policyType },
          policy
        );
        navigate(
          toPolicyDetails({
            realm,
            id,
            policyType,
            policyId: result.id!,
          })
        );
      }
      addAlert(
        t((policyId ? "update" : "create") + "PolicySuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("clients:policySaveError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:deletePolicy",
    messageKey: "clients:deletePolicyConfirm",
    continueButtonLabel: "clients:confirm",
    onConfirm: async () => {
      try {
        await adminClient.clients.delPolicy({
          id,
          policyId,
        });
        addAlert(t("policyDeletedSuccess"), AlertVariant.success);
        navigate(toAuthorizationTab({ realm, clientId: id, tab: "policies" }));
      } catch (error) {
        addError("clients:policyDeletedError", error);
      }
    },
  });

  if (policyId && !policy) {
    return <KeycloakSpinner />;
  }

  const ComponentType = isValidComponentType(policyType)
    ? COMPONENTS[policyType]
    : COMPONENTS["js"];

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={
          policyId
            ? policy?.name!
            : t("clients:createPolicyOfType", { policyType })
        }
        dropdownItems={
          policyId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-policy"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("common:delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="view-clients"
        >
          <FormProvider {...form}>
            <NameDescription prefix="policy" />
            <ComponentType />
            <LogicSelector />
          </FormProvider>
          <ActionGroup>
            <div className="pf-u-mt-md">
              <Button
                variant={ButtonVariant.primary}
                className="pf-u-mr-md"
                type="submit"
                data-testid="save"
              >
                {t("common:save")}
              </Button>

              <Button
                variant="link"
                data-testid="cancel"
                component={(props) => (
                  <Link
                    {...props}
                    to={toAuthorizationTab({
                      realm,
                      clientId: id,
                      tab: "policies",
                    })}
                  />
                )}
              >
                {t("common:cancel")}
              </Button>
            </div>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
