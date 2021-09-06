import React, { useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  DataList,
  Label,
  PageSection,
  Toolbar,
  ToolbarContent,
  ToggleGroup,
  ToggleGroupItem,
  AlertVariant,
} from "@patternfly/react-core";
import { CheckCircleIcon, TableIcon } from "@patternfly/react-icons";

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

  useFetch(
    async () => {
      const flows = await adminClient.authenticationManagement.getFlows();
      const flow = flows.find((f) => f.id === id);
      const executions =
        await adminClient.authenticationManagement.getExecutions({
          flow: flow?.alias!,
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
      const requests = [];
      for (let index = 0; index < Math.abs(times); index++) {
        if (times > 0) {
          requests.push(
            adminClient.authenticationManagement.lowerPriorityExecution({
              id,
            })
          );
        } else {
          requests.push(
            adminClient.authenticationManagement.raisePriorityExecution({
              id,
            })
          );
        }
      }
      await Promise.all(requests);
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
    execution: ExpandableExecution,
    type: AuthenticationProviderRepresentation
  ) => {
    try {
      await adminClient.authenticationManagement.addExecutionToFlow({
        flow: execution.displayName!,
        provider: type.id!,
      });
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updateFlowError", error);
    }
  };

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
        {executionList?.expandableList?.length && (
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
        {tableView && executionList?.expandableList?.length && (
          <>
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
                if (item.executionList && !item.isCollapsed) {
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
                    onAddExecution={addExecution}
                  />
                ))}
              </>
            </DataList>
            <div className="pf-screen-reader" aria-live="assertive">
              {liveText}
            </div>
          </>
        )}
        {!tableView && executionList?.expandableList && (
          <FlowDiagram executionList={executionList} />
        )}
        {!executionList?.expandableList ||
          (executionList.expandableList.length === 0 && (
            <EmptyExecutionState />
          ))}
      </PageSection>
    </>
  );
};
