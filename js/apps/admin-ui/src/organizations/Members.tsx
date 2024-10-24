import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { MemberModal } from "../groups/MembersModal";
import { toUser } from "../user/routes/User";
import { useParams } from "../utils/useParams";
import useToggle from "../utils/useToggle";
import { InviteMemberModal } from "./InviteMemberModal";
import { EditOrganizationParams } from "./routes/EditOrganization";
import { capitalizeFirstLetterFormatter } from "../util";
import { CheckboxFilterComponent } from "../components/dynamic/CheckboxFilterComponent";

type MembershipTypeRepresentation = UserRepresentation & {
  membershipType?: string;
};

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
  const { adminClient } = useAdminClient();
  const { id: orgId } = useParams<EditOrganizationParams>();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [open, toggle] = useToggle();
  const [openAddMembers, toggleAddMembers] = useToggle();
  const [openInviteMembers, toggleInviteMembers] = useToggle();
  const [selectedMembers, setSelectedMembers] = useState<UserRepresentation[]>(
    [],
  );
  const [filteredMembershipTypes, setFilteredMembershipTypes] = useState<
    string[]
  >([]);
  const [filterDisabled, setFilterDisabled] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  const membershipOptions = [
    { value: "Managed", label: "Managed" },
    { value: "Unmanaged", label: "Unmanaged" },
  ];

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const onSelect = (_event: any, value: string) => {
    if (filteredMembershipTypes.includes(value)) {
      setFilteredMembershipTypes(
        filteredMembershipTypes.filter((item) => item !== value),
      );
    } else {
      setFilteredMembershipTypes([...filteredMembershipTypes, value]);
    }
    setIsOpen(false);
    refresh();
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      const memberships: MembershipTypeRepresentation[] =
        await adminClient.organizations.listMembers({
          orgId,
          first,
          max,
          search,
        });

      const formattedMemberships = memberships.map((membership) => ({
        ...membership,
        membershipType: capitalizeFirstLetterFormatter()(
          membership.membershipType,
        ) as string,
      }));

      const hasManaged = formattedMemberships.some(
        (membership) =>
          membership.membershipType === membershipOptions[0].value,
      );
      const hasUnmanaged = formattedMemberships.some(
        (membership) =>
          membership.membershipType === membershipOptions[1].value,
      );

      setFilterDisabled(!(hasManaged && hasUnmanaged));

      if (filteredMembershipTypes.length === 0) {
        return formattedMemberships;
      }

      const filteredMemberships = formattedMemberships.filter((membership) =>
        filteredMembershipTypes.includes(membership.membershipType || ""),
      );

      return filteredMemberships;
    } catch (error) {
      addError("organizationsMembersListError", error);
      return [];
    }
  };

  const removeMember = async (selectedMembers: UserRepresentation[]) => {
    try {
      await Promise.all(
        selectedMembers.map((user) =>
          adminClient.organizations.delMember({
            orgId,
            userId: user.id!,
          }),
        ),
      );
      addAlert(t("organizationUsersLeft", { count: selectedMembers.length }));
    } catch (error) {
      addError("organizationUsersLeftError", error);
    }

    refresh();
  };

  return (
    <PageSection variant="light">
      {openAddMembers && (
        <MemberModal
          membersQuery={() => adminClient.organizations.listMembers({ orgId })}
          onAdd={async (selectedRows) => {
            try {
              await Promise.all(
                selectedRows.map((user) =>
                  adminClient.organizations.addMember({
                    orgId,
                    userId: user.id!,
                  }),
                ),
              );
              addAlert(
                t("organizationUsersAdded", { count: selectedRows.length }),
              );
            } catch (error) {
              addError("organizationUsersAddedError", error);
            }
          }}
          onClose={() => {
            toggleAddMembers();
            refresh();
          }}
        />
      )}
      {openInviteMembers && (
        <InviteMemberModal orgId={orgId} onClose={toggleInviteMembers} />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="membersList"
        searchPlaceholderKey="searchMember"
        onSelect={(members) => setSelectedMembers([...members])}
        canSelectAll
        toolbarItem={
          <>
            <ToolbarItem>
              <Dropdown
                onOpenChange={toggle}
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
                  <DropdownItem
                    onClick={() => {
                      toggleAddMembers();
                      toggle();
                    }}
                  >
                    {t("addRealmUser")}
                  </DropdownItem>
                  <DropdownItem
                    onClick={() => {
                      toggleInviteMembers();
                      toggle();
                    }}
                  >
                    {t("inviteMember")}
                  </DropdownItem>
                </DropdownList>
              </Dropdown>
            </ToolbarItem>
            <ToolbarItem>
              <Button
                variant="plain"
                isDisabled={selectedMembers.length === 0}
                onClick={() => removeMember(selectedMembers)}
              >
                {t("removeMember")}
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <CheckboxFilterComponent
                filterPlaceholderText={t("filterByMembershipType")}
                isDisabled={filterDisabled}
                isOpen={isOpen}
                options={membershipOptions}
                onOpenChange={(nextOpen) => setIsOpen(nextOpen)}
                onToggleClick={onToggleClick}
                onSelect={onSelect}
                selectedItems={filteredMembershipTypes}
                width={"290px"}
              />
            </ToolbarItem>
          </>
        }
        actions={[
          {
            title: t("remove"),
            onRowClick: async (member) => {
              await removeMember([member]);
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
          },
          {
            name: "firstName",
          },
          {
            name: "lastName",
          },
          {
            name: "membershipType",
            cellFormatters: [capitalizeFirstLetterFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyMembers")}
            instructions={t("emptyMembersInstructions")}
            secondaryActions={[
              {
                text: t("addRealmUser"),
                onClick: toggleAddMembers,
              },
              {
                text: t("inviteMember"),
                onClick: toggleInviteMembers,
              },
            ]}
          />
        }
      />
    </PageSection>
  );
};
