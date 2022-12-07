import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom-v5-compat";
import { Trans, useTranslation } from "react-i18next";
import {
  DataList,
  Label,
  PageSection,
  Toolbar,
  ToolbarItem,
  ToolbarContent,
  ToggleGroup,
  ToggleGroupItem,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  TableIcon,
  DomainIcon,
} from "@patternfly/react-icons";

import type AuthenticationExecutionInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationExecutionInfoRepresentation";
import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type { FlowParams } from "./routes/Flow";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { EmptyExecutionState } from "./EmptyExecutionState";
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
import { DuplicateFlowModal } from "./DuplicateFlowModal";
import { useRealm } from "../context/realm-context/RealmContext";
import useToggle from "../utils/useToggle";
import { toAuthentication } from "./routes/Authentication";
import { EditFlowModal } from "./EditFlowModal";
import { BindFlowDialog } from "./BindFlowDialog";

export const providerConditionFilter = (
  value: AuthenticationProviderRepresentation
) => value.displayName?.startsWith("Condition ");

export default function FlowDetails() {
  const { t } = useTranslation("authentication");
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { id, usedBy, builtIn } = useParams<FlowParams>();
  const navigate = useNavigate();
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
  const [open, toggleOpen, setOpen] = useToggle();
  const [edit, setEdit] = useState(false);
  const [bindFlowOpen, toggleBindFlow] = useToggle();

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

  const update = async (execution: ExpandableExecution) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { executionList, isCollapsed, ...ex } = execution;
    try {
      await adminClient.authenticationManagement.updateExecution(
        { flow: flow?.alias! },
        ex
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

  const [toggleDeleteFlow, DeleteFlowConfirm] = useConfirmDialog({
    titleKey: "authentication:deleteConfirmFlow",
    children: (
      <Trans i18nKey="authentication:deleteConfirmFlowMessage">
        {" "}
        <strong>{{ flow: flow?.alias || "" }}</strong>.
      </Trans>
    ),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.authenticationManagement.deleteFlow({
          flowId: flow!.id!,
        });
        navigate(toAuthentication({ realm }));
        addAlert(t("deleteFlowSuccess"), AlertVariant.success);
      } catch (error) {
        addError("authentication:deleteFlowError", error);
      }
    },
  });

  const hasExecutions = executionList?.expandableList.length !== 0;

  const dropdownItems = [
    ...(usedBy !== "DEFAULT"
      ? [
          <DropdownItem
            data-testid="set-as-default"
            key="default"
            onClick={toggleBindFlow}
          >
            {t("bindFlow")}
          </DropdownItem>,
        ]
      : []),
    <DropdownItem key="duplicate" onClick={() => setOpen(true)}>
      {t("duplicate")}
    </DropdownItem>,
    ...(!builtIn
      ? [
          <DropdownItem
            data-testid="edit-flow"
            key="edit"
            onClick={() => setEdit(true)}
          >
            {t("editInfo")}
          </DropdownItem>,
          <DropdownItem
            data-testid="delete-flow"
            key="delete"
            onClick={() => toggleDeleteFlow()}
          >
            {t("common:delete")}
          </DropdownItem>,
        ]
      : []),
  ];

  return (
    <>
      {bindFlowOpen && (
        <BindFlowDialog
          flowAlias={flow?.alias!}
          onClose={() => {
            toggleBindFlow();
            refresh();
          }}
        />
      )}
      {open && (
        <DuplicateFlowModal
          name={flow?.alias!}
          description={flow?.description!}
          toggleDialog={toggleOpen}
          onComplete={() => {
            refresh();
            setOpen(false);
          }}
        />
      )}
      {edit && (
        <EditFlowModal
          flow={flow!}
          toggleDialog={() => {
            setEdit(!edit);
            refresh();
          }}
        />
      )}
      <DeleteFlowConfirm />

      <ViewHeader
        titleKey={flow?.alias || ""}
        badges={[
          { text: <Label>{t(`used.${usedBy}`)}</Label> },
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
        dropdownItems={dropdownItems}
      />
      <PageSection variant="light">
        {executionList && hasExecutions && (
          <>
            <Toolbar id="toolbar">
              <ToolbarContent>
                <ToolbarItem>
                  <ToggleGroup>
                    <ToggleGroupItem
                      icon={<TableIcon />}
                      aria-label={t("tableView")}
                      buttonId="tableView"
                      isSelected={tableView}
                      onChange={() => setTableView(true)}
                    />
                    <ToggleGroupItem
                      icon={<DomainIcon />}
                      aria-label={t("diagramView")}
                      buttonId="diagramView"
                      isSelected={!tableView}
                      onChange={() => setTableView(false)}
                    />
                  </ToggleGroup>
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    data-testid="addStep"
                    variant="secondary"
                    onClick={() => setShowAddExecutionDialog(true)}
                  >
                    {t("addStep")}
                  </Button>
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    data-testid="addSubFlow"
                    variant="secondary"
                    onClick={() => setShowSubFlowDialog(true)}
                  >
                    {t("addSubFlow")}
                  </Button>
                </ToolbarItem>
              </ToolbarContent>
            </Toolbar>
            <DeleteConfirm />
            {tableView && (
              <DataList
                aria-label={t("flows")}
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
                      builtIn={!!builtIn}
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
                      onAddFlow={(execution, flow) =>
                        addFlow(execution.displayName!, flow)
                      }
                      onDelete={(execution) => {
                        setSelectedExecution(execution);
                        toggleDeleteDialog();
                      }}
                    />
                  ))}
                </>
              </DataList>
            )}
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
}
