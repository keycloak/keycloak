import {
  AlertVariant,
  Button,
  ButtonVariant,
  PageSection,
} from "@patternfly/react-core";
import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import WorkflowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/workflowRepresentation";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { ViewHeader } from "../components/view-header/ViewHeader";
//import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import { toAddWorkflow } from "./routes/AddWorkflow";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";

export default function WorkflowsSection() {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();

  // TODO: handle role-based access
  //const { hasAccess } = useAccess();
  //const isManager = hasAccess("manage-realm");

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [selectedWorkflow, setSelectedWorkflow] =
    useState<WorkflowRepresentation>();

  const loader = async () => {
    const workflows = await adminClient.workflows.find();
    return workflows.sort(
      (a: WorkflowRepresentation, b: WorkflowRepresentation) => {
        const nameA = a.name ?? "";
        const nameB = b.name ?? "";
        return nameA.localeCompare(nameB);
      },
    );
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "workflowDeleteConfirm",
    messageKey: t("workflowDeleteConfirmDialog", {
      selectedRoleName: selectedWorkflow ? selectedWorkflow!.name : "",
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.workflows.delById({ id: selectedWorkflow!.id! });
        setSelectedWorkflow(undefined);
        addAlert(t("workflowDeletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("workflowDeleteError", error);
      }
    },
  });

  return (
    <>
      <ViewHeader
        titleKey="titleWorkflows"
        subKey="workflowsExplain"
        helpUrl={helpUrls.workflowsUrl}
      />
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <DeleteConfirm />
        <KeycloakDataTable
          key={key}
          toolbarItem={
            <Button
              data-testid="create-workflow"
              component={(props) => (
                <Link {...props} to={toAddWorkflow({ realm })} />
              )}
            >
              {t("createWorkflow")}
            </Button>
          }
          columns={[
            {
              name: "name",
              displayKey: "name",
            },
            {
              name: "id",
              displayKey: "id",
            },
            {
              name: "enabled",
              displayKey: "enabled",
              cellRenderer: (row: WorkflowRepresentation) => {
                return (row.enabled ?? true) ? t("enabled") : t("disabled");
              },
            },
          ]}
          actions={[
            {
              title: t("delete"),
              onRowClick: (workflow) => {
                setSelectedWorkflow(workflow);
                toggleDeleteDialog();
              },
            } as Action<WorkflowRepresentation>,
          ]}
          loader={loader}
          ariaLabelKey="workflows"
          emptyState={
            <ListEmptyState
              message={t("emptyWorkflows")}
              instructions={t("emptyWorkflowsInstructions")}
              primaryActionText={t("createWorkflow")}
              onPrimaryAction={() => navigate(toAddWorkflow({ realm }))}
            />
          }
        />
      </PageSection>
    </>
  );
}
