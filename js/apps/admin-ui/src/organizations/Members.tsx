import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { Link } from "react-router-dom";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import useToggle from "../utils/useToggle";
import { useRealm } from "../context/realm-context/RealmContext";
import { toUser } from "../user/routes/User";

const UserDetailLink = (user: any) => {
  const { realm } = useRealm();
  return (
    <Link to={toUser({ realm, id: user.id!, tab: "settings" })}>
      {user.username}
    </Link>
  );
};

export const Members = () => {
  const { t } = useTranslation();

  // const [key, setKey] = useState(0);
  // const refresh = () => setKey(key + 1);

  const [open, toggle] = useToggle();
  const [selectedMembers, setSelectedMembers] = useState([]);

  const loader = () =>
    Promise.resolve([
      {
        id: "0ddbc3b9-ffa2-4800-89d2-7f272f049871",
        username: "a-member",
        email: "one@one.ch",
        firstName: "Alex",
        lastName: "Muster",
      },
    ]);
  return (
    <KeycloakDataTable
      // key={key}
      loader={loader}
      isPaginated
      ariaLabelKey="membersList"
      searchPlaceholderKey="searchMember"
      onSelect={(members) => selectedMembers([...members])}
      canSelectAll
      toolbarItem={
        <>
          <ToolbarItem>
            <Dropdown
              toggle={(ref) => (
                <MenuToggle
                  ref={ref}
                  onClick={toggle}
                  isExpanded={open}
                  variant="primary"
                >
                  {t("addMember")}
                </MenuToggle>
              )}
              isOpen={open}
            >
              <DropdownList>
                <DropdownItem>{t("addRealmUser")}</DropdownItem>
                <DropdownItem>{t("inviteMember")}</DropdownItem>
              </DropdownList>
            </Dropdown>
          </ToolbarItem>
          <ToolbarItem>
            <Button variant="plain" isDisabled={selectedMembers.length === 0}>
              {t("removeMember")}
            </Button>
          </ToolbarItem>
        </>
      }
      actions={[
        {
          title: t("delete"),
          onRowClick: (member) => {
            setSelectedMembers([member]);
            // toggleDeleteDialog();
          },
        },
      ]}
      columns={[
        {
          name: "username",
          cellRenderer: UserDetailLink,
        },
        {
          name: "email",
          // cellRenderer: Domains,
        },
        {
          name: "firstName",
        },
        {
          name: "lastName",
        },
      ]}
      emptyState={
        <ListEmptyState
          message={t("emptyOrganizations")}
          instructions={t("emptyOrganizationsInstructions")}
        />
      }
    />
  );
};
