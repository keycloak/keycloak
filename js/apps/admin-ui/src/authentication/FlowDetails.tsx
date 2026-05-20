import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import AuthenticatorConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {
  DndContext,
  DragEndEvent,
  DragMoveEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  Label,
  PageSection,
  ToggleGroup,
  ToggleGroupItem,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  CodeBranchIcon,
  CogIcon,
  DomainIcon,
  GripVerticalIcon,
  TableIcon,
} from "@patternfly/react-icons";
import { Table, Tbody } from "@patternfly/react-table";
import { useCallback, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import useToggle from "../utils/useToggle";
import { BindFlowDialog } from "./BindFlowDialog";
import { BuildInLabel } from "./BuildInLabel";
import { DuplicateFlowModal } from "./DuplicateFlowModal";
import { EditFlowModal } from "./EditFlowModal";
import { EmptyExecutionState } from "./EmptyExecutionState";
import { AuthenticationProviderContextProvider } from "./components/AuthenticationProviderContext";
import { FlowDiagram } from "./components/FlowDiagram";
import { FlowHeader } from "./components/FlowHeader";
import { FlowRow, DropMode } from "./components/FlowRow";
import { AddStepModal } from "./components/modals/AddStepModal";
import { AddSubFlowModal, Flow } from "./components/modals/AddSubFlowModal";
import {
  ExecutionList,
  ExpandableExecution,
  IndexChange,
  LevelChange,
} from "./execution-model";
import { toAuthentication } from "./routes/Authentication";
import { toFlow, type FlowParams } from "./routes/Flow";

export const providerConditionFilter = (
  value: AuthenticationProviderRepresentation,
) => value.displayName?.startsWith("Condition ");

const DragOverlayContent = ({
  execution,
}: {
  execution: ExpandableExecution;
}) => {
  const isSubflow = execution.authenticationFlow;
  return (
    <div className="keycloak__authentication__drag-overlay">
      <span className="keycloak__authentication__drag-overlay-handle">
        <GripVerticalIcon />
      </span>
      <span className="keycloak__authentication__drag-overlay-icon">
        {isSubflow ? <CodeBranchIcon /> : <CogIcon />}
      </span>
      <span className="keycloak__authentication__drag-overlay-text">
        {execution.displayName}
      </span>
    </div>
  );
};

export default function FlowDetails() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { id, usedBy, builtIn } = useParams<FlowParams>();
  const navigate = useNavigate();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [tableView, setTableView] = useState(true);
  const [flow, setFlow] = useState<AuthenticationFlowRepresentation>();
  const [executionList, setExecutionList] = useState<ExecutionList>();
  const [liveText, setLiveText] = useState("");

  const [showAddExecutionDialog, setShowAddExecutionDialog] =
    useState<boolean>();
  const [showAddSubFlowDialog, setShowSubFlowDialog] = useState<boolean>();
  const [selectedExecution, setSelectedExecution] =
    useState<ExpandableExecution>();
  const [open, toggleOpen, setOpen] = useToggle();
  const [edit, setEdit] = useState(false);
  const [bindFlowOpen, toggleBindFlow] = useToggle();
  const [activeId, setActiveId] = useState<string | null>(null);
  const [dropInfo, setDropInfo] = useState<{
    targetId: string | null;
    mode: DropMode;
  }>({ targetId: null, mode: "reorder" });

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    }),
  );

  const findExecutionById = useCallback(
    (
      id: string,
      list?: ExpandableExecution[],
    ): ExpandableExecution | undefined => {
      const searchList = list || executionList?.expandableList || [];
      for (const ex of searchList) {
        if (ex.id === id) {
          return ex;
        }
        if (ex.executionList) {
          const found = findExecutionById(id, ex.executionList);
          if (found) return found;
        }
      }
      return undefined;
    },
    [executionList],
  );

  const findSubflowById = useCallback(
    (
      id: string,
      list?: ExpandableExecution[],
    ): ExpandableExecution | undefined => {
      const searchList = list || executionList?.expandableList || [];
      for (const ex of searchList) {
        if (ex.id === id && ex.authenticationFlow) {
          return ex;
        }
        if (ex.executionList) {
          const found = findSubflowById(id, ex.executionList);
          if (found) return found;
        }
      }
      return undefined;
    },
    [executionList],
  );

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    setActiveId(active.id as string);

    const item = findExecutionById(active.id as string);
    if (item) {
      setLiveText(t("onDragStart", { item: item.displayName }));
      if (!item.isCollapsed && item.executionList?.length) {
        item.isCollapsed = true;
        setExecutionList(executionList!.clone());
      }
    }
  };

  const handleDragMove = (event: DragMoveEvent) => {
    const { activatorEvent, active } = event;
    const pointerEvent = activatorEvent as PointerEvent;

    const rows = document.querySelectorAll("tr[data-execution-id]");
    let foundTarget: { id: string; mode: DropMode } | null = null;

    const pointerX = pointerEvent.clientX + event.delta.x;
    const pointerY = pointerEvent.clientY + event.delta.y;

    for (const row of rows) {
      const rect = row.getBoundingClientRect();
      if (
        pointerX >= rect.left &&
        pointerX <= rect.right &&
        pointerY >= rect.top &&
        pointerY <= rect.bottom
      ) {
        const executionId = row.getAttribute("data-execution-id");
        const isSubflow = row.getAttribute("data-is-subflow") === "true";

        if (executionId && executionId !== active.id) {
          const rowHeight = rect.height;
          const relativeY = pointerY - rect.top;
          const edgeZone = rowHeight * 0.25;

          let mode: DropMode;
          if (relativeY < edgeZone) {
            mode = "reorder-before";
          } else if (relativeY > rowHeight - edgeZone) {
            mode = "reorder-after";
          } else if (isSubflow) {
            mode = "drop-into";
          } else {
            mode = "reorder-after";
          }

          foundTarget = { id: executionId, mode };
        }
        break;
      }
    }

    if (foundTarget) {
      setDropInfo({ targetId: foundTarget.id, mode: foundTarget.mode });
      const dragged = findExecutionById(active.id as string);
      if (dragged) {
        setLiveText(t("onDragMove", { item: dragged.displayName }));
      }
    } else {
      setDropInfo({ targetId: null, mode: "reorder" });
    }
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active } = event;
    setActiveId(null);

    const currentDropInfo = dropInfo;
    setDropInfo({ targetId: null, mode: "reorder" });

    if (!executionList) {
      setLiveText(t("onDragCancel"));
      return;
    }

    const activeId = active.id as string;
    const dragged = findExecutionById(activeId);

    if (!dragged) {
      setLiveText(t("onDragCancel"));
      return;
    }

    if (!currentDropInfo.targetId) {
      setLiveText(t("onDragCancel"));
      return;
    }

    setLiveText(t("onDragFinish", { list: dragged.displayName }));

    if (currentDropInfo.mode === "drop-into") {
      const targetSubflow = findSubflowById(currentDropInfo.targetId);
      if (targetSubflow && targetSubflow.id !== dragged.id) {
        const change = new LevelChange(
          targetSubflow.executionList?.length || 0,
          targetSubflow.index!,
          targetSubflow,
        );
        void executeChange(dragged, change);
        return;
      }
    }

    const order = executionList.order().map((ex) => ex.id!);
    const oldIndex = order.indexOf(activeId);
    const targetIndex = order.indexOf(currentDropInfo.targetId);

    if (oldIndex !== -1 && targetIndex !== -1 && oldIndex !== targetIndex) {
      const [removed] = order.splice(oldIndex, 1);
      let insertIndex = targetIndex;
      if (currentDropInfo.mode === "reorder-after") {
        insertIndex = oldIndex < targetIndex ? targetIndex : targetIndex + 1;
      } else {
        insertIndex = oldIndex < targetIndex ? targetIndex - 1 : targetIndex;
      }
      order.splice(insertIndex, 0, removed);
      const change = executionList.getChange(dragged, order);
      void executeChange(dragged, change);
    }
  };

  useFetch(
    async () => {
      const flows = await adminClient.authenticationManagement.getFlows();
      const flow = flows.find((f) => f.id === id);
      if (!flow) {
        throw new Error(t("notFound"));
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
    [key],
  );

  const executeChange = async (
    ex: AuthenticationFlowRepresentation | ExpandableExecution,
    change: LevelChange | IndexChange,
  ) => {
    try {
      let id = ex.id!;
      if ("parent" in change) {
        let config: AuthenticatorConfigRepresentation = {};
        if ("authenticationConfig" in ex) {
          config = await adminClient.authenticationManagement.getConfig({
            id: ex.authenticationConfig as string,
          });
        }

        try {
          await adminClient.authenticationManagement.delExecution({ id });
        } catch {
          // skipping already deleted execution
        }
        if ("authenticationFlow" in ex) {
          const executionFlow = ex as ExpandableExecution;
          const result =
            await adminClient.authenticationManagement.addFlowToFlow({
              flow: change.parent?.displayName! || flow?.alias!,
              alias: executionFlow.displayName!,
              description: executionFlow.description!,
              provider: ex.providerId!,
              type: "basic-flow",
            });
          id = result.id!;
          ex.executionList?.forEach((e, i) =>
            executeChange(e, {
              parent: { ...ex, id: result.id },
              newIndex: i,
              oldIndex: i,
            }),
          );
        } else {
          const result =
            await adminClient.authenticationManagement.addExecutionToFlow({
              flow: change.parent?.displayName! || flow?.alias!,
              provider: ex.providerId!,
            });

          if (config.id) {
            const newConfig = {
              id: result.id,
              alias: config.alias,
              config: config.config,
            };
            await adminClient.authenticationManagement.createConfig(newConfig);
          }

          id = result.id!;
        }
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
    } catch (error) {
      addError("updateFlowError", error);
    }
  };

  const update = async (execution: ExpandableExecution) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { executionList, isCollapsed, ...ex } = execution;
    try {
      await adminClient.authenticationManagement.updateExecution(
        { flow: flow?.alias! },
        ex,
      );
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateFlowError", error);
    }
  };

  const addExecution = async (
    name: string,
    type: AuthenticationProviderRepresentation,
  ) => {
    try {
      await adminClient.authenticationManagement.addExecutionToFlow({
        flow: name,
        provider: type.id!,
      });
      refresh();
      addAlert(t("updateFlowSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateFlowError", error);
    }
  };

  const addFlow = async (
    flow: string,
    { name, description = "", type, provider }: Flow,
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
      addError("updateFlowError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteConfirmExecution", {
      name: selectedExecution?.displayName,
    }),
    children: (
      <Trans i18nKey="deleteConfirmExecutionMessage">
        {" "}
        <strong>{{ name: selectedExecution?.displayName }}</strong>.
      </Trans>
    ),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.authenticationManagement.delExecution({
          id: selectedExecution?.id!,
        });
        addAlert(t("deleteExecutionSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("deleteExecutionError", error);
      }
    },
  });

  const [toggleDeleteFlow, DeleteFlowConfirm] = useConfirmDialog({
    titleKey: "deleteConfirmFlow",
    children: (
      <Trans i18nKey="deleteConfirmFlowMessage">
        {" "}
        <strong>{{ flow: flow?.alias || "" }}</strong>.
      </Trans>
    ),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.authenticationManagement.deleteFlow({
          flowId: flow!.id!,
        });
        navigate(toAuthentication({ realm }));
        addAlert(t("deleteFlowSuccess"), AlertVariant.success);
      } catch (error) {
        addError("deleteFlowError", error);
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
        ]
      : []),
    ...(!builtIn && !usedBy
      ? [
          <DropdownItem
            data-testid="delete-flow"
            key="delete"
            onClick={() => toggleDeleteFlow()}
          >
            {t("delete")}
          </DropdownItem>,
        ]
      : []),
  ];

  return (
    <AuthenticationProviderContextProvider>
      {bindFlowOpen && (
        <BindFlowDialog
          flowAlias={flow?.alias!}
          onClose={(usedBy) => {
            toggleBindFlow();
            navigate(
              toFlow({
                realm,
                id: id!,
                usedBy: usedBy ? "DEFAULT" : "notInUse",
                builtIn: builtIn ? "builtIn" : undefined,
              }),
            );
          }}
        />
      )}
      {open && flow && (
        <DuplicateFlowModal
          name={flow.alias!}
          description={flow.description!}
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
                text: <BuildInLabel />,
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
                    {t("addExecution")}
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
              <DndContext
                sensors={sensors}
                onDragStart={handleDragStart}
                onDragMove={handleDragMove}
                onDragEnd={handleDragEnd}
              >
                <Table aria-label={t("flows")} isTreeTable>
                  <FlowHeader />
                  <>
                    {executionList.expandableList.map((execution) => (
                      <Tbody key={execution.id}>
                        <FlowRow
                          builtIn={!!builtIn}
                          execution={execution}
                          dropInfo={dropInfo}
                          activeId={activeId}
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
                      </Tbody>
                    ))}
                  </>
                </Table>
                <DragOverlay dropAnimation={null}>
                  {activeId ? (
                    <DragOverlayContent
                      execution={
                        executionList.order().find((ex) => ex.id === activeId)!
                      }
                    />
                  ) : null}
                </DragOverlay>
              </DndContext>
            )}
            {flow && (
              <>
                {showAddExecutionDialog && (
                  <AddStepModal
                    name={flow.alias!}
                    type={
                      flow.providerId === "client-flow" ? "client" : "basic"
                    }
                    onSelect={async (type) => {
                      if (type) {
                        await addExecution(flow.alias!, type);
                      }
                      setShowAddExecutionDialog(false);
                    }}
                  />
                )}
                {showAddSubFlowDialog && (
                  <AddSubFlowModal
                    name={flow.alias!}
                    onCancel={() => setShowSubFlowDialog(false)}
                    onConfirm={async (newFlow) => {
                      await addFlow(flow.alias!, newFlow);
                      setShowSubFlowDialog(false);
                    }}
                  />
                )}
              </>
            )}
            <div className="pf-v5-screen-reader" aria-live="assertive">
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
    </AuthenticationProviderContextProvider>
  );
}
