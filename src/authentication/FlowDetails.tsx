import React, { useState } from "react";
import { useParams } from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import {
  DataList,
  Label,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToggleGroup,
  ToggleGroupItem,
  AlertVariant,
  ActionGroup,
  Button,
  ButtonVariant,
} from "@patternfly/react-core";
import { CheckCircleIcon, PlusIcon, TableIcon } from "@patternfly/react-icons";

import type AuthenticationExecutionInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationExecutionInfoRepresentation";
import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type { FlowParams } from "./routes/Flow";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { EmptyExecutionState } from "./EmptyExecutionState";
import { toUpperCase } from "../util";
import { FlowHeader } from "./components/FlowHeader";
import { FlowRow } from "./components/FlowRow";
import {
  ExecutionList,
  ExpandableExecution,
  IndexChange,
  LevelChange,
} from "./execution-model";
import { FlowDiagram } from "./components/FlowDiagram";
import { useAlerts } from "../components/alert/Alerts";
import { AddStepModal } from "./components/modals/AddStepModal";
import { AddSubFlowModal, Flow } from "./components/modals/AddSubFlowModal";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";

export const providerConditionFilter = (
  value: AuthenticationProviderRepresentation
) => value.displayName?.startsWith("Condition ");

export const FlowDetails = () => {
  const { t } = useTranslation("authentication");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { id, usedBy, builtIn } = useParams<FlowParams>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [tableView, setTableView] = useState(true);
  const [flow, setFlow] = useState<AuthenticationFlowRepresentation>();
  const [executionList, setExecutionList] = useState<ExecutionList>();
  const [dragged, setDragged] =
    useState<AuthenticationExecutionInfoRepresentation>();
  const [liveText, setLiveText] = useState("");

  const [showAddExecutionDialog, setShowAddExecutionDialog] =
    useState<boolean>();
  const [showAddSubFlowDialog, setShowSubFlowDialog] = useState<boolean>();
  const [selectedExecution, setSelectedExecution] =
    useState<ExpandableExecution>();

  useFetch(
    async () => {
      const flows = await adminClient.authenticationManagement.getFlows();
      const flow = flows.find((f) => f.id === id);
      if (!flow) {
        throw new Error(t("common:notFound"));
      }

      const executions =
        await adminClient.authenticationManagement.getExecutions({
          flow: flow.alias!,
        });
      return { flow, executions };
    },
    ({ flow, executions }) => {
      setFlow(flow);
      setExecutionList(new ExecutionList(executions));
    },
    [key]
  );

  const executeChange = async (
    ex: AuthenticationFlowRepresentation,
    change: LevelChange | IndexChange
  ) => {
    try {
      let id = ex.id!;
      if ("parent" in change) {
        await adminClient.authenticationManagement.delExecution({ id });
        const result =
          await adminClient.authenticationManagement.addExecutionToFlow({
            flow: change.parent?.displayName! || flow?.alias!,
            provider: ex.providerId!,
          });
        id = result.id!;
      }
      const times = change.newIndex - change.oldIndex;
      for (let index = 0; index < Math.abs(times); index++) {
        if (times > 0) {
          await adminClient.authenticationManagement.lowerPriorityExecution({
            id,
          });
        } else {
          await adminClient.authenticationManagement.raisePriorityExecution({
            id,
          });
        }
      }
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error: any) {
      addError("authentication:updateFlowError", error);
    }
  };

  const update = async (
    execution: AuthenticationExecutionInfoRepresentation
  ) => {
    try {
      await adminClient.authenticationManagement.updateExecution(
        { flow: flow?.alias! },
        execution
      );
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error: any) {
      addError("authentication:updateFlowError", error);
    }
  };

  const addExecution = async (
    name: string,
    type: AuthenticationProviderRepresentation
  ) => {
    try {
      await adminClient.authenticationManagement.addExecutionToFlow({
        flow: name,
        provider: type.id!,
      });
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updateFlowError", error);
    }
  };

  const addFlow = async (
    flow: string,
    { name, description = "", type, provider }: Flow
  ) => {
    try {
      await adminClient.authenticationManagement.addFlowToFlow({
        flow,
        alias: name,
        description,
        provider,
        type,
      });
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updateFlowError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "authentication:deleteConfirmExecution",
    children: (
      <Trans i18nKey="authentication:deleteConfirmExecutionMessage">
        {" "}
        <strong>{{ name: selectedExecution?.displayName }}</strong>.
      </Trans>
    ),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.authenticationManagement.delExecution({
          id: selectedExecution?.id!,
        });
        addAlert(t("deleteExecutionSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("authentication:deleteExecutionError", error);
      }
    },
  });

  const hasExecutions = executionList?.expandableList.length !== 0;

  return (
    <>
      <ViewHeader
        titleKey={toUpperCase(flow?.alias || "")}
        badges={[
          { text: <Label>{t(usedBy)}</Label> },
          builtIn
            ? {
                text: (
                  <Label
                    className="keycloak_authentication-section__usedby_label"
                    icon={<CheckCircleIcon />}
                  >
                    {t("buildIn")}
                  </Label>
                ),
                id: "builtIn",
              }
            : {},
        ]}
      />
      <PageSection variant="light">
        {hasExecutions && (
          <Toolbar id="toolbar">
            <ToolbarContent>
              <ToggleGroup>
                <ToggleGroupItem
                  icon={<TableIcon />}
                  aria-label={t("tableView")}
                  buttonId="tableView"
                  isSelected={tableView}
                  onChange={() => setTableView(true)}
                />
                <ToggleGroupItem
                  icon={<i className="fas fa-project-diagram"></i>}
                  aria-label={t("diagramView")}
                  buttonId="diagramView"
                  isSelected={!tableView}
                  onChange={() => setTableView(false)}
                />
              </ToggleGroup>
            </ToolbarContent>
          </Toolbar>
        )}
        {tableView && executionList && hasExecutions && (
          <>
            <DeleteConfirm />
            <DataList
              aria-label="flows"
              onDragFinish={(order) => {
                const withoutHeaderId = order.slice(1);
                setLiveText(
                  t("common:onDragFinish", { list: dragged?.displayName })
                );
                const change = executionList.getChange(
                  dragged!,
                  withoutHeaderId
                );
                executeChange(dragged!, change);
              }}
              onDragStart={(id) => {
                const item = executionList.findExecution(id)!;
                setLiveText(
                  t("common:onDragStart", { item: item.displayName })
                );
                setDragged(item);
                if (!item.isCollapsed) {
                  item.isCollapsed = true;
                  setExecutionList(executionList.clone());
                }
              }}
              onDragMove={() =>
                setLiveText(
                  t("common:onDragMove", { item: dragged?.displayName })
                )
              }
              onDragCancel={() => setLiveText(t("common:onDragCancel"))}
              itemOrder={[
                "header",
                ...executionList.order().map((ex) => ex.id!),
              ]}
            >
              <FlowHeader />
              <>
                {executionList.expandableList.map((execution) => (
                  <FlowRow
                    key={execution.id}
                    execution={execution}
                    onRowClick={(execution) => {
                      execution.isCollapsed = !execution.isCollapsed;
                      setExecutionList(executionList.clone());
                    }}
                    onRowChange={update}
                    onAddExecution={(execution, type) =>
                      addExecution(execution.displayName!, type)
                    }
                    onAddFlow={(flow) => addFlow(execution.displayName!, flow)}
                    onDelete={() => {
                      setSelectedExecution(execution);
                      toggleDeleteDialog();
                    }}
                  />
                ))}
              </>
            </DataList>
            {flow && (
              <>
                {showAddExecutionDialog && (
                  <AddStepModal
                    name={flow.alias!}
                    type={
                      flow.providerId === "client-flow" ? "client" : "basic"
                    }
                    onSelect={(type) => {
                      if (type) {
                        addExecution(flow.alias!, type);
                      }
                      setShowAddExecutionDialog(false);
                    }}
                  />
                )}
                {showAddSubFlowDialog && (
                  <AddSubFlowModal
                    name={flow.alias!}
                    onCancel={() => setShowSubFlowDialog(false)}
                    onConfirm={(newFlow) => {
                      addFlow(flow.alias!, newFlow);
                      setShowSubFlowDialog(false);
                    }}
                  />
                )}
                <ActionGroup>
                  <Button
                    data-testid="addStep"
                    variant="link"
                    onClick={() => setShowAddExecutionDialog(true)}
                  >
                    <PlusIcon /> {t("addStep")}
                  </Button>
                  <Button
                    data-testid="addSubFlow"
                    variant="link"
                    onClick={() => setShowSubFlowDialog(true)}
                  >
                    <PlusIcon /> {t("addSubFlow")}
                  </Button>
                </ActionGroup>
              </>
            )}
            <div className="pf-screen-reader" aria-live="assertive">
              {liveText}
            </div>
          </>
        )}
        {!tableView && executionList?.expandableList && (
          <FlowDiagram executionList={executionList} />
        )}
        {!executionList?.expandableList ||
          (flow && !hasExecutions && (
            <EmptyExecutionState
              flow={flow}
              onAddExecution={(type) => addExecution(flow.alias!, type)}
              onAddFlow={(newFlow) => addFlow(flow.alias!, newFlow)}
            />
          ))}
      </PageSection>
    </>
  );
};
