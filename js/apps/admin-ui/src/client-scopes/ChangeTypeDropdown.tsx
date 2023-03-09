import { useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant, Select } from "@patternfly/react-core";

import {
  allClientScopeTypes,
  changeClientScope,
  changeScope,
  ClientScope,
  clientScopeTypesSelectOptions,
} from "../components/client-scope/ClientScopeTypes";
import type { Row } from "../clients/scopes/ClientScopes";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";

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
  const { t } = useTranslation("client-scopes");
  const [open, setOpen] = useState(false);

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  return (
    <Select
      toggleId="change-type-dropdown"
      aria-label="change-type-to"
      isOpen={open}
      selections={[]}
      isDisabled={selectedRows.length === 0}
      placeholderText={t("changeTypeTo")}
      onToggle={setOpen}
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
                    value as ClientScope
                  )
                : changeScope(adminClient, row, value as ClientScope);
            })
          );
          setOpen(false);
          refresh();
          addAlert(t("clientScopeSuccess"), AlertVariant.success);
        } catch (error) {
          addError("clients:clientScopeError", error);
        }
      }}
    >
      {clientScopeTypesSelectOptions(
        t,
        !clientId ? allClientScopeTypes : undefined
      )}
    </Select>
  );
};
