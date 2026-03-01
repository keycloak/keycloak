import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { Button, ButtonVariant, ToolbarItem } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import useToggle from "../../utils/useToggle";
import { toRegistrationProvider } from "../routes/AddRegistrationProvider";
import { ClientRegistrationParams } from "../routes/ClientRegistration";
import { AddProviderDialog } from "./AddProviderDialog";

type ClientRegistrationListProps = {
  subType: "anonymous" | "authenticated";
};

const DetailLink = (comp: ComponentRepresentation) => {
  const { realm } = useRealm();
  const { subTab } = useParams<ClientRegistrationParams>();

  return (
    <Link
      key={comp.id}
      to={toRegistrationProvider({
        realm,
        subTab: subTab || "anonymous",
        providerId: comp.providerId!,
        id: comp.id,
      })}
    >
      {comp.name}
    </Link>
  );
};

export const ClientRegistrationList = ({
  subType,
}: ClientRegistrationListProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { subTab } = useParams<ClientRegistrationParams>();
  const navigate = useNavigate();

  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [policies, setPolicies] = useState<ComponentRepresentation[]>([]);
  const [selectedPolicy, setSelectedPolicy] =
    useState<ComponentRepresentation>();
  const [isAddDialogOpen, toggleAddDialog] = useToggle();

  useFetch(
    () =>
      adminClient.components.find({
        type: "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy",
      }),
    (policies) => setPolicies(policies.filter((p) => p.subType === subType)),
    [selectedPolicy],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clientRegisterPolicyDeleteConfirmTitle",
    messageKey: t("clientRegisterPolicyDeleteConfirm", {
      name: selectedPolicy?.name,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          realm,
          id: selectedPolicy?.id!,
        });
        addAlert(t("clientRegisterPolicyDeleteSuccess"));
        setSelectedPolicy(undefined);
      } catch (error) {
        addError("clientRegisterPolicyDeleteError", error);
      }
    },
  });

  return (
    <>
      {isAddDialogOpen && (
        <AddProviderDialog
          onConfirm={(providerId) =>
            navigate(
              toRegistrationProvider({
                realm,
                subTab: subTab || "anonymous",
                providerId,
              }),
            )
          }
          toggleDialog={toggleAddDialog}
        />
      )}
      <DeleteConfirm />
      <KeycloakDataTable
        ariaLabelKey="clientRegistration"
        searchPlaceholderKey={t("searchClientRegistration")}
        data-testid={`clientRegistration-${subType}`}
        loader={policies}
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid={`createPolicy-${subType}`}
              onClick={toggleAddDialog}
            >
              {t("createPolicy")}
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: t("delete"),
            onRowClick: (policy) => {
              setSelectedPolicy(policy);
              toggleDeleteDialog();
            },
          } as Action<ComponentRepresentation>,
        ]}
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: DetailLink,
          },
          {
            name: "providerId",
            displayKey: "providerId",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noAccessPolicies")}
            instructions={t("noAccessPoliciesInstructions")}
            primaryActionText={t("createPolicy")}
            onPrimaryAction={toggleAddDialog}
          />
        }
      />
    </>
  );
};
