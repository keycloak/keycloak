import React from "react";
import { useTranslation } from "react-i18next";
import { Alert, AlertVariant } from "@patternfly/react-core";

import type { ExpandableScopeRepresentation } from "./Scopes";
import { useAlerts } from "../../components/alert/Alerts";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";

type DeleteScopeDialogProps = {
  clientId: string;
  selectedScope:
    | ExpandableScopeRepresentation
    | ScopeRepresentation
    | undefined;
  refresh: () => void;
  open: boolean;
  toggleDialog: () => void;
};

export const DeleteScopeDialog = ({
  clientId,
  selectedScope,
  refresh,
  open,
  toggleDialog,
}: DeleteScopeDialogProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  return (
    <ConfirmDialogModal
      open={open}
      toggleDialog={toggleDialog}
      titleKey="clients:deleteScope"
      continueButtonLabel="clients:confirm"
      onConfirm={async () => {
        try {
          await adminClient.clients.delAuthorizationScope({
            id: clientId,
            scopeId: selectedScope?.id!,
          });
          addAlert(t("resourceScopeSuccess"), AlertVariant.success);
          refresh();
        } catch (error) {
          addError("clients:resourceScopeError", error);
        }
      }}
    >
      {t("deleteScopeConfirm")}
      {selectedScope &&
        "permissions" in selectedScope &&
        selectedScope.permissions &&
        selectedScope.permissions.length > 0 && (
          <Alert
            variant="warning"
            isInline
            isPlain
            title={t("deleteScopeWarning")}
            className="pf-u-pt-lg"
          >
            <p className="pf-u-pt-xs">
              {selectedScope.permissions.map((permission) => (
                <strong key={permission.id} className="pf-u-pr-md">
                  {permission.name}
                </strong>
              ))}
            </p>
          </Alert>
        )}
    </ConfirmDialogModal>
  );
};
