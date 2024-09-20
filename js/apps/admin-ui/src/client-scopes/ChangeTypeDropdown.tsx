import {
  AlertVariant,
  MenuToggle,
  Select,
  SelectList,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import type { Row } from "../clients/scopes/ClientScopes";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  ClientScope,
  allClientScopeTypes,
  changeClientScope,
  changeScope,
  clientScopeTypesSelectOptions,
} from "../components/client-scope/ClientScopeTypes";

type ChangeTypeDropdownProps = {
  clientId?: string;
  selectedRows: Row[];
  refresh: () => void;
};

export const ChangeTypeDropdown = ({
  clientId,
  selectedRows,
  refresh,
}: ChangeTypeDropdownProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const { addAlert, addError } = useAlerts();

  return (
    <Select
      aria-label="change-type-to"
      onOpenChange={(isOpen) => setOpen(isOpen)}
      isOpen={open}
      toggle={(ref) => (
        <MenuToggle
          id="change-type-dropdown"
          isDisabled={selectedRows.length === 0}
          ref={ref}
          onClick={() => setOpen(!open)}
          isExpanded={open}
        >
          {t("changeTypeTo")}
        </MenuToggle>
      )}
      onSelect={async (_, value) => {
        try {
          await Promise.all(
            selectedRows.map((row) => {
              return clientId
                ? changeClientScope(
                    adminClient,
                    clientId,
                    row,
                    row.type,
                    value as ClientScope,
                  )
                : changeScope(adminClient, row, value as ClientScope);
            }),
          );
          setOpen(false);
          refresh();
          addAlert(t("clientScopeSuccess"), AlertVariant.success);
        } catch (error) {
          addError("clientScopeError", error);
        }
      }}
    >
      <SelectList>
        {clientScopeTypesSelectOptions(
          t,
          !clientId ? allClientScopeTypes : undefined,
        )}
      </SelectList>
    </Select>
  );
};
