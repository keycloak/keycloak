import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { Tooltip } from "@patternfly/react-core";
import {
  Dropdown,
  DropdownItem,
  DropdownToggle,
} from "@patternfly/react-core/deprecated";
import { PlusIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import type { ExpandableExecution } from "../execution-model";
import { AddStepModal, FlowType } from "./modals/AddStepModal";
import { AddSubFlowModal, Flow } from "./modals/AddSubFlowModal";

type AddFlowDropdownProps = {
  execution: ExpandableExecution;
  onAddExecution: (
    execution: ExpandableExecution,
    type: AuthenticationProviderRepresentation,
  ) => void;
  onAddFlow: (execution: ExpandableExecution, flow: Flow) => void;
};

export const AddFlowDropdown = ({
  execution,
  onAddExecution,
  onAddFlow,
}: AddFlowDropdownProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const [open, setOpen] = useState(false);
  const [type, setType] = useState<FlowType>();
  const [providerId, setProviderId] = useState<string>();

  useFetch(
    () =>
      adminClient.authenticationManagement.getFlow({
        flowId: execution.flowId!,
      }),
    ({ providerId }) => setProviderId(providerId),
    [],
  );

  return (
    <Tooltip content={t("add")}>
      <>
        <Dropdown
          isPlain
          position="right"
          data-testid={`${execution.displayName}-edit-dropdown`}
          isOpen={open}
          toggle={
            <DropdownToggle
              onToggle={(_event, val) => setOpen(val)}
              aria-label={t("add")}
            >
              <PlusIcon />
            </DropdownToggle>
          }
          dropdownItems={[
            <DropdownItem
              key="addStep"
              onClick={() =>
                setType(providerId === "form-flow" ? "form" : "basic")
              }
            >
              {t("addStep")}
            </DropdownItem>,
            <DropdownItem
              key="addCondition"
              onClick={() => setType("condition")}
            >
              {t("addCondition")}
            </DropdownItem>,
            <DropdownItem key="addSubFlow" onClick={() => setType("subFlow")}>
              {t("addSubFlow")}
            </DropdownItem>,
          ]}
          onSelect={() => setOpen(false)}
        />
        {type && type !== "subFlow" && (
          <AddStepModal
            name={execution.displayName!}
            type={type}
            onSelect={(type) => {
              if (type) {
                onAddExecution(execution, type);
              }
              setType(undefined);
            }}
          />
        )}
        {type === "subFlow" && (
          <AddSubFlowModal
            name={execution.displayName!}
            onCancel={() => setType(undefined)}
            onConfirm={(flow) => {
              onAddFlow(execution, flow);
              setType(undefined);
            }}
          />
        )}
      </>
    </Tooltip>
  );
};
