import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type Groups from "@keycloak/keycloak-admin-client/lib/resources/groups";
import { useTranslation } from "react-i18next";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { GroupPickerDialog } from "../../components/group/GroupPickerDialog";
import { useGroupResource } from "../../context/group-resource/GroupResourceContext";

type MoveDialogProps = {
  source: GroupRepresentation;
  onClose: () => void;
  refresh: () => void;
};

const moveToRoot = (
  groupsResource: Groups.Groups,
  source: GroupRepresentation,
) =>
  source.id ? groupsResource.updateRoot(source) : groupsResource.create(source);

const moveToGroup = async (
  groupsResource: Groups.Groups,
  source: GroupRepresentation,
  dest: GroupRepresentation,
) => groupsResource.updateChildGroup({ id: dest.id! }, source);

export const MoveDialog = ({ source, onClose, refresh }: MoveDialogProps) => {
  const groupsResource = useGroupResource();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const moveGroup = async (group?: GroupRepresentation[]) => {
    try {
      await (group
        ? moveToGroup(groupsResource, source, group[0])
        : moveToRoot(groupsResource, source));
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
