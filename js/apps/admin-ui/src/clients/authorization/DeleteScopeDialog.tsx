import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import { Alert, AlertVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import type { PermissionScopeRepresentation } from "./Scopes";

type DeleteScopeDialogProps = {
  clientId: string;
  selectedScope:
    | PermissionScopeRepresentation
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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  return (
    <ConfirmDialogModal
      open={open}
      toggleDialog={toggleDialog}
      titleKey="deleteScope"
      continueButtonLabel="confirm"
      onConfirm={async () => {
        try {
          await adminClient.clients.delAuthorizationScope({
            id: clientId,
            scopeId: selectedScope?.id!,
          });
          addAlert(t("resourceScopeSuccess"), AlertVariant.success);
          refresh();
        } catch (error) {
          addError("resourceScopeError", error);
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
            className="pf-v5-u-pt-lg"
          >
            <p className="pf-v5-u-pt-xs">
              {selectedScope.permissions.map((permission) => (
                <strong key={permission.id} className="pf-v5-u-pr-md">
                  {permission.name}
                </strong>
              ))}
            </p>
          </Alert>
        )}
    </ConfirmDialogModal>
  );
};
