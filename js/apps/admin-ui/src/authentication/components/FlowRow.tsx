import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { Button, Draggable, Tooltip } from "@patternfly/react-core";
import { TrashIcon } from "@patternfly/react-icons";
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
      <Draggable key={`draggable-${execution.id}`} hasNoWrapper>
        <TreeRowWrapper
          row={{ props: treeRow.props }}
          className="keycloak__authentication__flow-row"
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
      </Draggable>
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
