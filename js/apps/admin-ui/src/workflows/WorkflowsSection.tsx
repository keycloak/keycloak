import {
  AlertVariant,
  Button,
  ButtonVariant,
  PageSection,
  Switch,
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
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { toWorkflowDetail } from "./routes/WorkflowDetail";

export default function WorkflowsSection() {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();

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

  const toggleEnabled = async (workflow: WorkflowRepresentation) => {
    const enabled = !(workflow.enabled ?? true);
    const workflowToUpdate = { ...workflow, enabled };
    try {
      await adminClient.workflows.update(
        { id: workflow.id! },
        workflowToUpdate,
      );

      addAlert(
        workflowToUpdate.enabled ? t("workflowEnabled") : t("workflowDisabled"),
        AlertVariant.success,
      );
      refresh();
    } catch (error) {
      addError("workflowUpdateError", error);
    }
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
                <Link
                  {...props}
                  to={toWorkflowDetail({ realm, mode: "create", id: "new" })}
                />
              )}
            >
              {t("createWorkflow")}
            </Button>
          }
          columns={[
            {
              name: "name",
              displayKey: "name",
              cellRenderer: (row: WorkflowRepresentation) => (
                <Link
                  to={toWorkflowDetail({ realm, mode: "update", id: row.id! })}
                >
                  {row.name}
                </Link>
              ),
            },
            {
              name: "id",
              displayKey: "id",
            },
            {
              name: "status",
              displayKey: "status",
              cellRenderer: (workflow: WorkflowRepresentation) => (
                <Switch
                  label={t("enabled")}
                  labelOff={t("disabled")}
                  isChecked={workflow.enabled ?? true}
                  onChange={() => toggleEnabled(workflow)}
                />
              ),
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
            {
              title: t("copy"),
              onRowClick: (workflow) => {
                setSelectedWorkflow(workflow);
                navigate(
                  toWorkflowDetail({ realm, mode: "copy", id: workflow.id! }),
                );
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
              onPrimaryAction={() =>
                navigate(toWorkflowDetail({ realm, mode: "create", id: "new" }))
              }
            />
          }
        />
      </PageSection>
    </>
  );
}
