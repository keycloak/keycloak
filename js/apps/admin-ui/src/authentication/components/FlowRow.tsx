import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { Button, Tooltip } from "@patternfly/react-core";
import { css } from "@patternfly/react-styles";
import styles from "@patternfly/react-styles/css/components/Table/table";
import stylesTreeView from "@patternfly/react-styles/css/components/Table/table-tree-view";
import { TrashIcon } from "@patternfly/react-icons";
import { Td, Tr } from "@patternfly/react-table";
import { DragEvent } from "react";
import { useTranslation } from "react-i18next";
import type { ExpandableExecution } from "../execution-model";
import { AddFlowDropdown } from "./AddFlowDropdown";
import { EditFlow } from "./EditFlow";
import { ExecutionConfigModal } from "./ExecutionConfigModal";
import { useFlowDragDrop } from "./FlowDragDropContext";
import { FlowRequirementDropdown } from "./FlowRequirementDropdown";
import { FlowTitle } from "./FlowTitle";
import type { Flow } from "./modals/AddSubFlowModal";

import "./flow-row.css";

type FlowRowProps = {
  builtIn: boolean;
  execution: ExpandableExecution;
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
  onRowClick,
  onRowChange,
  onAddExecution,
  onAddFlow,
  onDelete,
}: FlowRowProps) => {
  const { t } = useTranslation();
  const dragDrop = useFlowDragDrop();
  const hasSubList = !!execution.executionList?.length;

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

  return (
    <>
      <Tr
        key={`draggable-${execution.id}`}
        id={execution.id}
        aria-level={treeRow.props["aria-level"]}
        aria-labelledby={treeRow.props["aria-labelledby"]}
        aria-setsize={treeRow.props["aria-setsize"]}
        aria-expanded={treeRow.props.isExpanded}
        className={css(
          "keycloak__authentication__flow-row",
          treeRow.props.isExpanded && styles.modifiers.expanded,
          treeRow.props.isDetailsExpanded &&
            stylesTreeView.modifiers.treeViewDetailsExpanded,
        )}
        draggable={!!dragDrop}
        onDragStart={(event: DragEvent<HTMLTableRowElement>) =>
          dragDrop?.onRowDragStart(event, execution.id!)
        }
        onDragOver={(event: DragEvent<HTMLTableRowElement>) =>
          dragDrop?.onRowDragOver(event, execution.id!)
        }
        onDrop={(event: DragEvent<HTMLTableRowElement>) =>
          dragDrop?.onRowDrop(event, execution.id!)
        }
        onDragEnd={(event: DragEvent<HTMLTableRowElement>) =>
          dragDrop?.onRowDragEnd(event)
        }
      >
        <Td
          draggableRow={{
            id: execution.id!,
          }}
        />
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
                icon={<TrashIcon />}
                variant="plain"
                data-testid={`${execution.displayName}-delete`}
                aria-label={t("delete")}
                onClick={() => onDelete(execution)}
              />
            </Tooltip>
          )}
        </Td>
      </Tr>
      {!execution.isCollapsed &&
        hasSubList &&
        execution.executionList?.map((ex) => (
          <FlowRow
            builtIn={builtIn}
            key={ex.id}
            execution={ex}
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
