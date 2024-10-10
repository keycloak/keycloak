import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  ListEmptyState,
  OrganizationTable,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { OrganizationModal } from "../organizations/OrganizationModal";
import { toEditOrganization } from "../organizations/routes/EditOrganization";
import useToggle from "../utils/useToggle";
import { UserParams } from "./routes/User";
import { toUsers } from "./routes/Users";

export const Organizations = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { id } = useParams<UserParams>();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [joinToggle, toggle, setJoinToggle] = useToggle();
  const [shouldJoin, setShouldJoin] = useState(true);
  const [openOrganizationPicker, setOpenOrganizationPicker] = useState(false);
  const [userOrgs, setUserOrgs] = useState<OrganizationRepresentation[]>([]);
  const [selectedOrgs, setSelectedOrgs] = useState<
    OrganizationRepresentation[]
  >([]);

  useFetch(
    () => adminClient.organizations.memberOrganizations({ userId: id! }),
    setUserOrgs,
    [key],
  );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "removeConfirmOrganizationTitle",
    messageKey: t("organizationRemoveConfirm", { count: selectedOrgs.length }),
    continueButtonLabel: "remove",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await Promise.all(
          selectedOrgs.map((org) =>
            adminClient.organizations.delMember({
              orgId: org.id!,
              userId: id!,
            }),
          ),
        );
        addAlert(t("organizationRemovedSuccess"));
        const user = await adminClient.users.findOne({ id: id! });
        if (!user) {
          navigate(toUsers({ realm: realm }));
        }
        setSelectedOrgs([]);
        refresh();
      } catch (error) {
        addError("organizationRemoveError", error);
      }
    },
  });

  return (
    <>
      {openOrganizationPicker && (
        <OrganizationModal
          isJoin={shouldJoin}
          existingOrgs={userOrgs}
          onClose={() => setOpenOrganizationPicker(false)}
          onAdd={async (orgs) => {
            try {
              await Promise.all(
                orgs.map((org) => {
                  const form = new FormData();
                  form.append("id", id!);
                  return shouldJoin
                    ? adminClient.organizations.addMember({
                        orgId: org.id!,
                        userId: id!,
                      })
                    : adminClient.organizations.inviteExistingUser(
                        {
                          orgId: org.id!,
                        },
                        form,
                      );
                }),
              );
              addAlert(
                t(
                  shouldJoin
                    ? "userAddedOrganization"
                    : "userInvitedOrganization",
                  { count: orgs.length },
                ),
              );
              refresh();
            } catch (error) {
              addError(
                shouldJoin ? "userAddedOrganizationError" : "userInvitedError",
                error,
              );
            }
          }}
        />
      )}
      <DeleteConfirm />
      <OrganizationTable
        link={({ organization, children }) => (
          <Link
            key={organization.id}
            to={toEditOrganization({
              realm,
              id: organization.id!,
              tab: "settings",
            })}
          >
            {children}
          </Link>
        )}
        loader={userOrgs}
        onSelect={(orgs) => setSelectedOrgs(orgs)}
        deleteLabel="remove"
        onDelete={(org) => {
          setSelectedOrgs([org]);
          toggleDeleteDialog();
        }}
        toolbarItem={
          <>
            <ToolbarItem>
              <Dropdown
                onOpenChange={setJoinToggle}
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    id="toggle-id"
                    onClick={toggle}
                    variant="primary"
                  >
                    {t("joinOrganization")}
                  </MenuToggle>
                )}
                isOpen={joinToggle}
              >
                <DropdownList>
                  <DropdownItem
                    key="join"
                    onClick={() => {
                      setShouldJoin(true);
                      setOpenOrganizationPicker(true);
                    }}
                  >
                    {t("joinOrganization")}
                  </DropdownItem>
                  <DropdownItem
                    key="invite"
                    onClick={() => {
                      setShouldJoin(false);
                      setOpenOrganizationPicker(true);
                    }}
                  >
                    {t("sendInvite")}
                  </DropdownItem>
                </DropdownList>
              </Dropdown>
            </ToolbarItem>
            <ToolbarItem>
              <Button
                data-testid="removeOrganization"
                variant="secondary"
                isDisabled={selectedOrgs.length === 0}
                onClick={() => toggleDeleteDialog()}
              >
                {t("remove")}
              </Button>
            </ToolbarItem>
          </>
        }
      >
        <ListEmptyState
          message={t("emptyUserOrganizations")}
          instructions={t("emptyUserOrganizationsInstructions")}
          secondaryActions={[
            {
              text: t("joinOrganization"),
              onClick: () => {
                setShouldJoin(true);
                setOpenOrganizationPicker(true);
              },
            },
            {
              text: t("sendInvitation"),
              onClick: () => {
                setShouldJoin(false);
                setOpenOrganizationPicker(true);
              },
            },
          ]}
        />
      </OrganizationTable>
    </>
  );
};
