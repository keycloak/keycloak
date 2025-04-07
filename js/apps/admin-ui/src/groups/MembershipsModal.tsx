import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { Modal, ModalVariant } from "@patternfly/react-core";
import {
  Button,
  ButtonVariant,
  Checkbox,
  Popover,
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { useHelp } from "@keycloak/keycloak-ui-shared";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { sortBy, uniqBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { GroupPath } from "../components/group/GroupPath";

type CredentialDataDialogProps = {
  user: UserRepresentation;
  onClose: () => void;
};

export const MembershipsModal = ({
  user,
  onClose,
}: CredentialDataDialogProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [isDirectMembership, setDirectMembership] = useState(true);
  const { enabled } = useHelp();
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

  return (
    <Modal
      variant={ModalVariant.large}
      title={t("showMembershipsTitle", { username: user.username })}
      data-testid="showMembershipsDialog"
      isOpen
      onClose={onClose}
      actions={[
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.primary}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        key={key}
        loader={loader}
        className="keycloak_user-section_groups-table"
        isPaginated
        ariaLabelKey="roleList"
        searchPlaceholderKey="searchGroup"
        toolbarItem={
          <>
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
        ]}
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noGroupMemberships")}
            instructions={t("noGroupMembershipsText")}
          />
        }
      />
    </Modal>
  );
};
