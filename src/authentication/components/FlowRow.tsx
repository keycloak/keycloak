import React from "react";
import { useTranslation } from "react-i18next";
import {
  DataListItemRow,
  DataListControl,
  DataListDragButton,
  DataListItemCells,
  DataListCell,
  DataListItem,
  DataListToggle,
  Text,
  TextVariants,
} from "@patternfly/react-core";

import type AuthenticationExecutionInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationExecutionInfoRepresentation";
import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type { ExpandableExecution } from "../execution-model";
import { FlowTitle } from "./FlowTitle";
import { FlowRequirementDropdown } from "./FlowRequirementDropdown";
import { ExecutionConfigModal } from "./ExecutionConfigModal";
import { EditFlowDropdown } from "./EditFlowDropdown";

import "./flow-row.css";

type FlowRowProps = {
  execution: ExpandableExecution;
  onRowClick: (execution: ExpandableExecution) => void;
  onRowChange: (execution: AuthenticationExecutionInfoRepresentation) => void;
  onAddExecution: (
    execution: ExpandableExecution,
    type: AuthenticationProviderRepresentation
  ) => void;
};

export const FlowRow = ({
  execution,
  onRowClick,
  onRowChange,
  onAddExecution,
}: FlowRowProps) => {
  const { t } = useTranslation("authentication");
  const hasSubList = !!execution.executionList?.length;

  return (
    <>
      <DataListItem
        className="keycloak__authentication__flow-item"
        id={execution.id}
        isExpanded={!execution.isCollapsed}
      >
        <DataListItemRow
          className="keycloak__authentication__flow-row"
          aria-level={execution.level}
        >
          <DataListControl>
            <DataListDragButton
              aria-labelledby={execution.displayName}
              aria-describedby={t("common-help:dragHelp")}
            />
          </DataListControl>
          {hasSubList && (
            <DataListToggle
              onClick={() => onRowClick(execution)}
              isExpanded={!execution.isCollapsed}
              id={`toggle1-${execution.id}`}
              aria-controls={`expand-${execution.id}`}
            />
          )}
          <DataListItemCells
            dataListCells={[
              <DataListCell key={`${execution.id}-name`}>
                {!hasSubList && (
                  <FlowTitle
                    key={execution.id}
                    title={execution.displayName!}
                  />
                )}
                {hasSubList && (
                  <>
                    {execution.displayName} <br />{" "}
                    <Text component={TextVariants.small}>
                      {execution.description}
                    </Text>
                  </>
                )}
              </DataListCell>,
              <DataListCell key={`${execution.id}-requirement`}>
                <FlowRequirementDropdown
                  flow={execution}
                  onChange={onRowChange}
                />
              </DataListCell>,
              <DataListCell key={`${execution.id}-config`}>
                {execution.configurable && (
                  <ExecutionConfigModal execution={execution} />
                )}
                {execution.authenticationFlow && (
                  <EditFlowDropdown
                    execution={execution}
                    onAddExecution={onAddExecution}
                  />
                )}
              </DataListCell>,
            ]}
          />
        </DataListItemRow>
      </DataListItem>
      {!execution.isCollapsed && hasSubList && (
        <>
          {execution.executionList.map((execution) => (
            <FlowRow
              key={execution.id}
              execution={execution}
              onRowClick={onRowClick}
              onRowChange={onRowChange}
              onAddExecution={onAddExecution}
            />
          ))}
        </>
      )}
    </>
  );
};
