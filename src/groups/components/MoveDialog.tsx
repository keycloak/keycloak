import { useTranslation } from "react-i18next";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { GroupPickerDialog } from "../../components/group/GroupPickerDialog";
import { useAdminClient } from "../../context/auth/AdminClient";

type MoveDialogProps = {
  source: GroupRepresentation;
  onClose: () => void;
  refresh: () => void;
};

const moveToRoot = async (
  adminClient: KeycloakAdminClient,
  source: GroupRepresentation
) => {
  await adminClient.groups.del({ id: source.id! });
  const { id } = await adminClient.groups.create({
    ...source,
    id: undefined,
  });
  if (source.subGroups) {
    await Promise.all(
      source.subGroups.map((s) =>
        adminClient.groups.setOrCreateChild(
          { id: id! },
          {
            ...s,
            id: undefined,
          }
        )
      )
    );
  }
};

const moveToGroup = async (
  adminClient: KeycloakAdminClient,
  source: GroupRepresentation,
  dest: GroupRepresentation
) => {
  try {
    await adminClient.groups.setOrCreateChild({ id: dest.id! }, source);
  } catch (error: any) {
    if (error.response) {
      throw error;
    }
  }
};

export const MoveDialog = ({ source, onClose, refresh }: MoveDialogProps) => {
  const { t } = useTranslation("groups");

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const moveGroup = async (group?: GroupRepresentation[]) => {
    try {
      await (group
        ? moveToGroup(adminClient, source, group[0])
        : moveToRoot(adminClient, source));
      refresh();
      addAlert(t("moveGroupSuccess"));
    } catch (error) {
      addError("groups:moveGroupError", error);
    }
  };

  return (
    <GroupPickerDialog
      type="selectOne"
      filterGroups={[source.name!]}
      text={{
        title: "groups:moveToGroup",
        ok: "groups:moveHere",
      }}
      onClose={onClose}
      onConfirm={moveGroup}
    />
  );
};
