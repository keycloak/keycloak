import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useHelp } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  Popover,
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { intersectionBy, sortBy, uniqBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { GroupPath } from "../components/group/GroupPath";
import { GroupPickerDialog } from "../components/group/GroupPickerDialog";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useAccess } from "../context/access/Access";

type UserGroupsProps = {
  user: UserRepresentation;
};

export const UserGroups = ({ user }: UserGroupsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [selectedGroups, setSelectedGroups] = useState<GroupRepresentation[]>(
    [],
  );

  const [isDirectMembership, setDirectMembership] = useState(true);
  const [directMembershipList, setDirectMembershipList] = useState<
    GroupRepresentation[]
  >([]);
  const [open, setOpen] = useState(false);

  const { enabled } = useHelp();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users");

  const alphabetize = (groupsList: GroupRepresentation[]) => {
    return sortBy(groupsList, (group) => group.path?.toUpperCase());
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };

    const searchParam = search || "";
    if (searchParam) {
      params.search = searchParam;
    }

    const joinedUserGroups = await adminClient.users.listGroups({
      ...params,
      id: user.id!,
    });

    setDirectMembershipList([...joinedUserGroups]);

    const indirect: GroupRepresentation[] = [];
    if (!isDirectMembership)
      joinedUserGroups.forEach((g) => {
        const paths = (
          g.path?.substring(1).match(/((~\/)|[^/])+/g) || []
        ).slice(0, -1);

        indirect.push(
          ...paths.map((p) => ({
            name: p,
            path: g.path?.substring(0, g.path.indexOf(p) + p.length),
          })),
        );
      });

    return alphabetize(uniqBy([...joinedUserGroups, ...indirect], "path"));
  };

  const toggleModal = () => {
    setOpen(!open);
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("leaveGroup", {
      count: selectedGroups.length,
      name: selectedGroups[0]?.name,
    }),
    messageKey: t("leaveGroupConfirmDialog", {
      count: selectedGroups.length,
      groupname: selectedGroups[0]?.name,
      username: user.username,
    }),
    continueButtonLabel: "leave",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await Promise.all(
          selectedGroups.map((group) =>
            adminClient.users.delFromGroup({
              id: user.id!,
              groupId: group.id!,
            }),
          ),
        );

        setSelectedGroups([]);
        addAlert(t("removedGroupMembership"), AlertVariant.success);
      } catch (error) {
        addError("removedGroupMembershipError", error);
      }
      refresh();
    },
  });

  const leave = (group: GroupRepresentation[]) => {
    setSelectedGroups(group);
    toggleDeleteDialog();
  };

  const addGroups = async (groups: GroupRepresentation[]): Promise<void> => {
    try {
      await Promise.all(
        groups.map((group) =>
          adminClient.users.addToGroup({
            id: user.id!,
            groupId: group.id!,
          }),
        ),
      );

      addAlert(t("addedGroupMembership"), AlertVariant.success);
    } catch (error) {
      addError("addedGroupMembershipError", error);
    }
    refresh();
  };

  return (
    <>
      <DeleteConfirm />
      {open && (
        <GroupPickerDialog
          id={user.id}
          type="selectMany"
          text={{
            title: t("joinGroupsFor", { username: user.username }),
            ok: "join",
          }}
          canBrowse={isManager}
          onClose={() => setOpen(false)}
          onConfirm={async (groups = []) => {
            await addGroups(groups);
            setOpen(false);
          }}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        className="keycloak_user-section_groups-table"
        isPaginated
        ariaLabelKey="roleList"
        searchPlaceholderKey="searchGroup"
        canSelectAll
        onSelect={(groups) =>
          isDirectMembership
            ? setSelectedGroups(groups)
            : setSelectedGroups(
                intersectionBy(groups, directMembershipList, "id"),
              )
        }
        isRowDisabled={(group) =>
          !isDirectMembership &&
          directMembershipList.every((item) => item.id !== group.id)
        }
        toolbarItem={
          <>
            <Button
              className="kc-join-group-button"
              onClick={toggleModal}
              data-testid="add-group-button"
              isDisabled={!user.access?.manageGroupMembership}
            >
              {t("joinGroup")}
            </Button>
            <Checkbox
              label={t("directMembership")}
              key="direct-membership-check"
              id="kc-direct-membership-checkbox"
              onChange={() => {
                setDirectMembership(!isDirectMembership);
                refresh();
              }}
              isChecked={isDirectMembership}
              className="pf-v5-u-mt-sm"
            />
            <Button
              onClick={() => leave(selectedGroups)}
              data-testid="leave-group-button"
              variant="link"
              isDisabled={selectedGroups.length === 0}
              className="pf-v5-u-ml-md"
            >
              {t("leave")}
            </Button>

            {enabled && (
              <Popover
                aria-label="Basic popover"
                position="bottom"
                bodyContent={<div>{t("whoWillAppearPopoverTextUsers")}</div>}
              >
                <Button
                  variant="link"
                  className="kc-who-will-appear-button"
                  key="who-will-appear-button"
                  icon={<QuestionCircleIcon />}
                >
                  {t("whoWillAppearLinkTextUsers")}
                </Button>
              </Popover>
            )}
          </>
        }
        columns={[
          {
            name: "groupMembership",
            displayKey: "groupMembership",
            cellRenderer: (group: GroupRepresentation) => group.name || "-",
            transforms: [cellWidth(40)],
          },
          {
            name: "path",
            displayKey: "path",
            cellRenderer: (group: GroupRepresentation) => (
              <GroupPath group={group} />
            ),
            transforms: [cellWidth(45)],
          },

          {
            name: "",
            cellRenderer: (group: GroupRepresentation) => {
              const canLeaveGroup =
                directMembershipList.some((item) => item.id === group.id) ||
                directMembershipList.length === 0 ||
                isDirectMembership;
              return canLeaveGroup ? (
                <Button
                  data-testid={`leave-${group.name}`}
                  onClick={() => leave([group])}
                  variant="link"
                  isDisabled={!user.access?.manageGroupMembership}
                >
                  {t("leave")}
                </Button>
              ) : (
                "-"
              );
            },
            transforms: [cellWidth(20)],
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noGroups")}
            instructions={t("noGroupsText")}
            primaryActionText={t("joinGroup")}
            onPrimaryAction={toggleModal}
          />
        }
      />
    </>
  );
};
