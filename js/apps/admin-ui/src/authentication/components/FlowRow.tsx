import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { useDraggable } from "@dnd-kit/core";
import { Button, Tooltip } from "@patternfly/react-core";
import { GripVerticalIcon, TrashIcon } from "@patternfly/react-icons";
import { Td, TreeRowWrapper } from "@patternfly/react-table";
import { useTranslation } from "react-i18next";
import type { ExpandableExecution } from "../execution-model";
import { AddFlowDropdown } from "./AddFlowDropdown";
import { EditFlow } from "./EditFlow";
import { ExecutionConfigModal } from "./ExecutionConfigModal";
import { FlowRequirementDropdown } from "./FlowRequirementDropdown";
import { FlowTitle } from "./FlowTitle";
import type { Flow } from "./modals/AddSubFlowModal";

import "./flow-row.css";

export type DropMode =
  | "reorder"
  | "reorder-before"
  | "reorder-after"
  | "drop-into";

type DropInfo = {
  targetId: string | null;
  mode: DropMode;
};

type FlowRowProps = {
  builtIn: boolean;
  execution: ExpandableExecution;
  dropInfo?: DropInfo;
  activeId?: string | null;
  onRowClick: (execution: ExpandableExecution) => void;
  onRowChange: (execution: ExpandableExecution) => void;
  onAddExecution: (
    execution: ExpandableExecution,
    type: AuthenticationProviderRepresentation,
  ) => void;
  onAddFlow: (execution: ExpandableExecution, flow: Flow) => void;
  onDelete: (execution: ExpandableExecution) => void;
};

export type FlowType = "flow" | "condition" | "execution" | "step";

const convertToType = (execution: ExpandableExecution): FlowType => {
  if (execution.authenticationFlow) {
    return "flow";
  }
  if (execution.displayName!.startsWith("Condition -")) {
    return "condition";
  }
  if (execution.level === 0) {
    return "execution";
  }
  return "step";
};

export const FlowRow = ({
  builtIn,
  execution,
  dropInfo,
  activeId,
  onRowClick,
  onRowChange,
  onAddExecution,
  onAddFlow,
  onDelete,
}: FlowRowProps) => {
  const { t } = useTranslation();
  const hasSubList = !!execution.executionList?.length;
  const isSubflow = execution.authenticationFlow;

  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: execution.id!,
  });

  const treeRow = {
    onCollapse: () => onRowClick(execution),
    props: {
      isExpanded: !execution.isCollapsed,
      isDetailsExpanded: !execution.isCollapsed,
      "aria-level": execution.level! + 1,
      "aria-labelledby": execution.id,
      "aria-setsize": hasSubList ? execution.executionList!.length : 0,
    },
  };

  const isDropTarget = dropInfo?.targetId === execution.id;
  const dropMode = isDropTarget ? dropInfo?.mode : undefined;
  const isBeingDragged = isDragging || activeId === execution.id;

  const getDropClassName = () => {
    const classes = ["keycloak__authentication__flow-row"];
    if (isBeingDragged) classes.push("is-dragging");
    return classes.join(" ");
  };

  return (
    <>
      <TreeRowWrapper
        row={{ props: treeRow.props }}
        className={getDropClassName()}
        data-execution-id={execution.id}
        data-is-subflow={isSubflow ? "true" : "false"}
        data-drop-mode={dropMode}
      >
        <Td className="keycloak__authentication__drag-cell">
          <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            className="keycloak__authentication__drag-handle"
          >
            <GripVerticalIcon />
          </div>
        </Td>
        <Td treeRow={treeRow}>
          <FlowTitle
            id={execution.id}
            type={convertToType(execution)}
            key={execution.id}
            subtitle={
              (execution.authenticationFlow
                ? execution.description
                : execution.alias) || ""
            }
            providerId={execution.providerId!}
            title={execution.displayName!}
          />
        </Td>
        <Td>
          <FlowRequirementDropdown flow={execution} onChange={onRowChange} />
        </Td>
        {(!execution.authenticationFlow || builtIn) && (
          <>
            <Td isActionCell />
            <Td isActionCell />
          </>
        )}
        <Td isActionCell>
          <ExecutionConfigModal execution={execution} />
        </Td>

        {execution.authenticationFlow && !builtIn && (
          <>
            <Td isActionCell>
              <AddFlowDropdown
                execution={execution}
                onAddExecution={onAddExecution}
                onAddFlow={onAddFlow}
              />
            </Td>
            <Td isActionCell>
              <EditFlow execution={execution} onRowChange={onRowChange} />
            </Td>
          </>
        )}
        <Td isActionCell>
          {!builtIn && (
            <Tooltip content={t("delete")}>
              <Button
                variant="plain"
                data-testid={`${execution.displayName}-delete`}
                aria-label={t("delete")}
                onClick={() => onDelete(execution)}
              >
                <TrashIcon />
              </Button>
            </Tooltip>
          )}
        </Td>
      </TreeRowWrapper>
      {!execution.isCollapsed &&
        hasSubList &&
        execution.executionList?.map((ex) => (
          <FlowRow
            builtIn={builtIn}
            key={ex.id}
            execution={ex}
            dropInfo={dropInfo}
            activeId={activeId}
            onRowClick={onRowClick}
            onRowChange={onRowChange}
            onAddExecution={onAddExecution}
            onAddFlow={onAddFlow}
            onDelete={onDelete}
          />
        ))}
    </>
  );
};
