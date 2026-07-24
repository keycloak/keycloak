import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  Button,
  Tooltip,
  EmptyStateHeader,
  EmptyStateFooter,
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";

import { PermissionType, toNewPermission } from "../routes/NewPermission";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toUpperCase } from "../../util";

type EmptyButtonProps = {
  permissionType: PermissionType;
  disabled?: boolean;
  clientId: string;
};

const EmptyButton = ({
  permissionType,
  disabled = false,
  clientId,
}: EmptyButtonProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const navigate = useNavigate();
  return (
    <Button
      data-testid={`create-${permissionType}`}
      className={
        disabled ? "keycloak__permissions__empty_state " : "pf-v5-u-m-sm"
      }
      variant="secondary"
      onClick={() =>
        !disabled &&
        navigate(toNewPermission({ realm, id: clientId, permissionType }))
      }
    >
      {t(`create${toUpperCase(permissionType)}BasedPermission`)}
    </Button>
  );
};

const TooltipEmptyButton = ({
  permissionType,
  disabled,
  ...props
}: EmptyButtonProps) => {
  const { t } = useTranslation();
  return disabled ? (
    <Tooltip content={t(`no${toUpperCase(permissionType)}CreateHint`)}>
      <EmptyButton
        {...props}
        disabled={disabled}
        permissionType={permissionType}
      />
    </Tooltip>
  ) : (
    <EmptyButton
      {...props}
      disabled={disabled}
      permissionType={permissionType}
    />
  );
};

type EmptyPermissionsStateProps = {
  clientId: string;
  isResourceEnabled?: boolean;
  isScopeEnabled?: boolean;
};

export const EmptyPermissionsState = ({
  clientId,
  isResourceEnabled,
  isScopeEnabled,
}: EmptyPermissionsStateProps) => {
  const { t } = useTranslation();
  return (
    <EmptyState data-testid="empty-state" variant="lg">
      <EmptyStateHeader
        titleText={<>{t("emptyPermissions")}</>}
        icon={<EmptyStateIcon icon={PlusCircleIcon} />}
        headingLevel="h1"
      />
      <EmptyStateBody>{t("emptyPermissionInstructions")}</EmptyStateBody>
      <EmptyStateFooter>
        <TooltipEmptyButton
          permissionType="resource"
          disabled={isResourceEnabled}
          clientId={clientId}
        />
        <br />
        <TooltipEmptyButton
          permissionType="scope"
          disabled={isScopeEnabled}
          clientId={clientId}
        />
      </EmptyStateFooter>
    </EmptyState>
  );
};
