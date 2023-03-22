import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Tooltip,
} from "@patternfly/react-core";
import { PlusIcon } from "@patternfly/react-icons";

import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import type { ExpandableExecution } from "../execution-model";
import { AddStepModal, FlowType } from "./modals/AddStepModal";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { AddSubFlowModal, Flow } from "./modals/AddSubFlowModal";

type AddFlowDropdownProps = {
  execution: ExpandableExecution;
  onAddExecution: (
    execution: ExpandableExecution,
    type: AuthenticationProviderRepresentation
  ) => void;
  onAddFlow: (execution: ExpandableExecution, flow: Flow) => void;
};

export const AddFlowDropdown = ({
  execution,
  onAddExecution,
  onAddFlow,
}: AddFlowDropdownProps) => {
  const { t } = useTranslation("authentication");
  const { adminClient } = useAdminClient();

  const [open, setOpen] = useState(false);
  const [type, setType] = useState<FlowType>();
  const [providerId, setProviderId] = useState<string>();

  useFetch(
    () =>
      adminClient.authenticationManagement.getFlow({
        flowId: execution.flowId!,
      }),
    ({ providerId }) => setProviderId(providerId),
    []
  );

  return (
    <Tooltip content={t("common:add")}>
      <>
        <Dropdown
          isPlain
          position="right"
          data-testid={`${execution.displayName}-edit-dropdown`}
          isOpen={open}
          toggle={
            <DropdownToggle onToggle={setOpen} aria-label={t("common:add")}>
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
