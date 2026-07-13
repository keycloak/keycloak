import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { useDraggable } from "@dnd-kit/core";
import { Button, Tooltip } from "@patternfly/react-core";
import { GripVerticalIcon, TrashIcon } from "@patternfly/react-icons";
import { Td, TreeRowWrapper } from "@patternfly/react-table";
import { useTranslation } from "react-i18next";
import type { DropInfo, ExpandableExecution } from "../execution-model";
import { AddFlowDropdown } from "./AddFlowDropdown";
import { EditFlow } from "./EditFlow";
import { ExecutionConfigModal } from "./ExecutionConfigModal";
import { FlowRequirementDropdown } from "./FlowRequirementDropdown";
import { FlowTitle } from "./FlowTitle";
import type { Flow } from "./modals/AddSubFlowModal";

import "./flow-row.css";

export type { DropInfo } from "../execution-model";

type FlowRowProps = {
  builtIn: boolean;
  execution: ExpandableExecution;
  dropInfo?: DropInfo;
  visualIndexById?: Map<string, number>;
  activeId?: string | null;
  pendingExpandId?: string | null;
  onRowClick: (execution: ExpandableExecution) => void;
  onRowChange: (execution: ExpandableExecution) => void;
  onAddExecution: (
    execution: ExpandableExecution,
    type: AuthenticationProviderRepresentation,
  ) => void;
  onAddFlow: (execution: ExpandableExecution, flow: Flow) => void;
  onDelete: (execution: ExpandableExecution) => void;
};

export type FlowType = "flow" | "condition" | "execution";

const convertToType = (execution: ExpandableExecution): FlowType => {
  if (execution.authenticationFlow) {
    return "flow";
  }
  if (execution.displayName!.startsWith("Condition -")) {
    return "condition";
  }
  return "execution";
};

export const FlowRow = ({
  builtIn,
  execution,
  dropInfo,
  visualIndexById,
  activeId,
  pendingExpandId,
  onRowClick,
  onRowChange,
  onAddExecution,
  onAddFlow,
  onDelete,
}: FlowRowProps) => {
  const { t } = useTranslation();
  const hasSubList = !!execution.executionList?.length;
  const isSubflow = execution.authenticationFlow;
  const level = execution.level || 0;
  const visualIndex = visualIndexById?.get(execution.id!);

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
  const isParentTarget =
    dropInfo?.targetParentId === execution.id &&
    dropInfo?.targetId !== execution.id;
  const isBeingDragged = isDragging || activeId === execution.id;

  const showDropLineBefore =
    isDropTarget &&
    dropMode === "reorder-before" &&
    dropInfo?.insertIndex === visualIndex;
  const showDropLineAfter =
    isDropTarget &&
    dropMode === "reorder-after" &&
    visualIndex !== undefined &&
    dropInfo?.insertIndex === visualIndex + 1;

  const getDropClassName = () => {
    const classes = ["keycloak__authentication__flow-row"];
    if (isBeingDragged) classes.push("is-dragging");
    if (isParentTarget) classes.push("keycloak__authentication__drop-parent");
    if (pendingExpandId === execution.id) {
      classes.push("keycloak__authentication__drop-pending-expand");
    }
    if (dropMode === "drop-into") {
      classes.push("keycloak__authentication__drop-into");
    }
    if (showDropLineBefore) {
      classes.push("keycloak__authentication__drop-line--before");
    }
    if (showDropLineAfter) {
      classes.push("keycloak__authentication__drop-line--after");
    }
    return classes.join(" ");
  };

  return (
    <>
      <TreeRowWrapper
        row={{ props: treeRow.props }}
        className={getDropClassName()}
        data-execution-id={execution.id}
        data-is-subflow={isSubflow ? "true" : "false"}
        data-level={level}
        data-drop-mode={
          isDropTarget ? dropMode : isParentTarget ? "nest-parent" : undefined
        }
        data-target-level={isDropTarget ? dropInfo?.targetLevel : undefined}
        data-target-parent-id={dropInfo?.targetParentId ?? undefined}
      >
        <Td className="keycloak__authentication__drag-cell">
          <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            className="keycloak__authentication__drag-handle"
            aria-label={t("dragHandle")}
          >
            <GripVerticalIcon />
          </div>
        </Td>
        <Td
          treeRow={treeRow}
          className="keycloak__authentication__flow-title-cell"
        >
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
            visualIndexById={visualIndexById}
            activeId={activeId}
            pendingExpandId={pendingExpandId}
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
