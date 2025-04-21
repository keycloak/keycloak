import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { GroupPickerDialog } from "../../components/group/GroupPickerDialog";

type MoveDialogProps = {
  source: GroupRepresentation;
  onClose: () => void;
  refresh: () => void;
};

const moveToRoot = (
  adminClient: KeycloakAdminClient,
  source: GroupRepresentation,
) =>
  source.id
    ? adminClient.groups.updateRoot(source)
    : adminClient.groups.create(source);

const moveToGroup = async (
  adminClient: KeycloakAdminClient,
  source: GroupRepresentation,
  dest: GroupRepresentation,
) => adminClient.groups.updateChildGroup({ id: dest.id! }, source);

export const MoveDialog = ({ source, onClose, refresh }: MoveDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const moveGroup = async (group?: GroupRepresentation[]) => {
    try {
      await (group
        ? moveToGroup(adminClient, source, group[0])
        : moveToRoot(adminClient, source));
      refresh();
      addAlert(t("moveGroupSuccess"));
    } catch (error) {
      addError("moveGroupError", error);
    }
  };

  return (
    <GroupPickerDialog
      type="selectOne"
      filterGroups={[source]}
      text={{
        title: "moveToGroup",
        ok: "moveHere",
      }}
      onClose={onClose}
      onConfirm={moveGroup}
      isMove
    />
  );
};
