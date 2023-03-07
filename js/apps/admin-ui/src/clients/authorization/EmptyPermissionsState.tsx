import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  EmptyState,
  EmptyStateIcon,
  Title,
  EmptyStateBody,
  Button,
  Tooltip,
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
  const { t } = useTranslation("clients");
  const { realm } = useRealm();
  const navigate = useNavigate();
  return (
    <Button
      data-testid={`create-${permissionType}`}
      className={
        disabled ? "keycloak__permissions__empty_state " : "" + "pf-u-m-sm"
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
  const { t } = useTranslation("clients");
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
  const { t } = useTranslation("clients");
  return (
    <EmptyState data-testid="empty-state" variant="large">
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title headingLevel="h1" size="lg">
        {t("emptyPermissions")}
      </Title>
      <EmptyStateBody>{t("emptyPermissionInstructions")}</EmptyStateBody>
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
    </EmptyState>
  );
};
