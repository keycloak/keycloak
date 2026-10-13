import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Tooltip,
} from "@patternfly/react-core";
import { PlusIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
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
          popperProps={{
            position: "right",
          }}
          isOpen={open}
          onOpenChange={(isOpen) => setOpen(isOpen)}
          toggle={(ref) => (
            <MenuToggle
              ref={ref}
              variant="plain"
              onClick={() => setOpen(!open)}
              aria-label={t("add")}
              data-testid={`${execution.displayName}-edit-dropdown`}
            >
              <PlusIcon />
            </MenuToggle>
          )}
          onSelect={() => setOpen(false)}
        >
          <DropdownList>
            <DropdownItem
              key="addStep"
              onClick={() =>
                setType(providerId === "form-flow" ? "form" : "basic")
              }
            >
              {t("addExecution")}
            </DropdownItem>
            <DropdownItem
              key="addCondition"
              onClick={() => setType("condition")}
            >
              {t("addCondition")}
            </DropdownItem>
            <DropdownItem key="addSubFlow" onClick={() => setType("subFlow")}>
              {t("addSubFlow")}
            </DropdownItem>
          </DropdownList>
        </Dropdown>
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
