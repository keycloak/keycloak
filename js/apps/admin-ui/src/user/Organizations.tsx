import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
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
import { useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { OrganizationModal } from "../organizations/OrganizationModal";
import { OrganizationTable } from "../organizations/OrganizationTable";
import useToggle from "../utils/useToggle";
import { UserParams } from "./routes/User";

export const Organizations = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { id } = useParams<UserParams>();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [joinToggle, toggle, setJoinToggle] = useToggle();
  const [joinOrganization, setJoinOrganization] = useState(false);
  const [selectedOrgs, setSelectedOrgs] = useState<
    OrganizationRepresentation[]
  >([]);

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
        refresh();
      } catch (error) {
        addError("organizationRemoveError", error);
      }
    },
  });

  return (
    <>
      {joinOrganization && (
        <OrganizationModal
          onClose={() => setJoinOrganization(false)}
          onAdd={async (orgs) => {
            try {
              await Promise.all(
                orgs.map((org) =>
                  adminClient.organizations.addMember({
                    orgId: org.id!,
                    userId: id!,
                  }),
                ),
              );
              addAlert(t("userAddedOrganization", { count: orgs.length }));
              refresh();
            } catch (error) {
              addError("userAddedOrganizationError", error);
            }
          }}
        />
      )}
      <DeleteConfirm />
      <OrganizationTable
        key={key}
        loader={() =>
          adminClient.organizations.memberOrganizations({
            userId: id!,
          })
        }
        onSelect={(orgs) => setSelectedOrgs(orgs)}
        deleteLabel="remove"
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
                      setJoinOrganization(true);
                    }}
                  >
                    {t("joinOrganization")}
                  </DropdownItem>
                  <DropdownItem key="invite" component="button">
                    {t("invite")}
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
              onClick: () => alert("join organization"),
            },
            {
              text: t("sendInvitation"),
              onClick: () => alert("send invitation"),
            },
          ]}
        />
      </OrganizationTable>
    </>
  );
};
