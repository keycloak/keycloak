import type { OrganizationInvitationRepresentation } from "@keycloak/keycloak-admin-client";
import { OrganizationInvitationStatus } from "@keycloak/keycloak-admin-client";
import {
  Label,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownList,
  Form,
  MenuToggle,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
  ToolbarItem,
} from "@patternfly/react-core";

import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { ClientSelect } from "../components/client/ClientSelect";
import { CheckboxFilterComponent } from "../components/dynamic/CheckboxFilterComponent";
import {
  useAlerts,
  ListEmptyState,
  KeycloakDataTable,
} from "@keycloak/keycloak-ui-shared";
import { useParams } from "../utils/useParams";
import useToggle from "../utils/useToggle";
import { InviteMemberModal } from "./InviteMemberModal";
import { MemberModal } from "../groups/MembersModal";
import { EditOrganizationParams } from "./routes/EditOrganization";
import { SearchInputComponent } from "../components/dynamic/SearchInputComponent";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import useFormatDate from "../utils/useFormatDate";

const InvitationStatusBadge = ({
  status,
}: {
  status?: OrganizationInvitationStatus;
}) => {
  const { t } = useTranslation();

  return (
    <Label variant="outline">
      {status ? t(`organizationInvitationStatus.${status.toLowerCase()}`) : ""}
    </Label>
  );
};

const DateCell = ({ date }: { date?: number }) => {
  const formatDate = useFormatDate();

  if (!date) {
    return <span>-</span>;
  }

  try {
    return <span>{formatDate(new Date(date * 1000))}</span>;
  } catch {
    return <span>{date}</span>;
  }
};

type SelectClientModalProps = {
  onSelect: (clientId: string) => void;
  onClose: () => void;
};

const SelectClientModal = ({ onSelect, onClose }: SelectClientModalProps) => {
  const { t } = useTranslation();
  const form = useForm<{ clientId: string }>();
  const { handleSubmit } = form;
  const clientId = form.watch("clientId");

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen
      onClose={onClose}
      aria-label={t("selectInvitationClient")}
    >
      <ModalHeader title={t("selectInvitationClient")} />
      <ModalBody>
        <FormProvider {...form}>
          <Form
            id="select-client-form"
            onSubmit={handleSubmit((data) => onSelect(data.clientId))}
          >
            <ClientSelect
              name="clientId"
              label="client"
              helpText="invitationClientHelp"
            />
          </Form>
        </FormProvider>
      </ModalBody>
      <ModalFooter>
        <Button
          data-testid="next"
          key="confirm"
          variant="primary"
          onClick={handleSubmit((data) => onSelect(data.clientId))}
        >
          {clientId ? t("next") : t("SKIP")}
        </Button>
        <Button key="cancel" variant={ButtonVariant.link} onClick={onClose}>
          {t("cancel")}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export const Invitations = () => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { id: orgId } = useParams<EditOrganizationParams>();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [openInviteMembers, toggleInviteMembers] = useToggle();
  const [openInviteRealmUser, toggleInviteRealmUser] = useToggle();
  const [openSelectClient, toggleSelectClient] = useToggle();
  const [inviteClientId, setInviteClientId] = useState<string>("");
  const [isInviteMenuOpen, setIsInviteMenuOpen] = useState(false);
  const [selectedInvitations, setSelectedInvitations] = useState<
    OrganizationInvitationRepresentation[]
  >([]);
  const [searchText, setSearchText] = useState<string>("");
  const [searchTriggerText, setSearchTriggerText] = useState<string>("");
  const [filteredStatuses, setFilteredStatuses] = useState<string[]>([]);
  const [isStatusFilterOpen, setIsStatusFilterOpen] = useState(false);

  const statusOptions = Object.values(OrganizationInvitationStatus).map(
    (status: string) => ({
      value: status,
      label: t(`organizationInvitationStatus.${status.toLowerCase()}`),
    }),
  );

  const loader = async (first?: number, max?: number) => {
    try {
      const invitations: OrganizationInvitationRepresentation[] =
        await adminClient.organizations.listInvitations({
          orgId,
          first,
          max,
          search: searchTriggerText,
          status:
            filteredStatuses.length === 1 ? filteredStatuses[0] : undefined,
        });

      return invitations;
    } catch (error) {
      addError("organizationsInvitationsListError", error);
      return [];
    }
  };

  const handleSearch = () => {
    setSearchTriggerText(searchText);
    refresh();
  };

  const clearSearch = () => {
    setSearchText("");
    setSearchTriggerText("");
    refresh();
  };

  const resendInvitation = async (
    invitation: OrganizationInvitationRepresentation,
  ) => {
    try {
      await adminClient.organizations.resendInvitation({
        orgId,
        invitationId: invitation.id!,
      });
      addAlert(t("organizationInvitationResent"));
      refresh();
    } catch (error) {
      addError("organizationInvitationResendError", error);
    }
  };

  const deleteInvitations = async (
    invitations: OrganizationInvitationRepresentation[],
  ) => {
    try {
      await Promise.all(
        invitations.map((invitation) =>
          adminClient.organizations.deleteInvitation({
            orgId,
            invitationId: invitation.id!,
          }),
        ),
      );
      addAlert(
        t("organizationInvitationsDeleted", { count: invitations.length }),
      );
      refresh();
    } catch (error) {
      addError("organizationInvitationsDeleteError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "organizationInvitationsDeleteConfirmTitle",
    messageKey: "organizationInvitationsDeleteConfirm",
    continueButtonLabel: "delete",
    onConfirm: () => deleteInvitations(selectedInvitations),
  });

  const onStatusFilterSelect = (
    _event: React.MouseEvent<HTMLButtonElement>,
    value: string,
  ) => {
    if (filteredStatuses.includes(value)) {
      setFilteredStatuses(
        filteredStatuses.filter((status) => status !== value),
      );
    } else {
      setFilteredStatuses([...filteredStatuses, value]);
    }
    setIsStatusFilterOpen(false);
    refresh();
  };

  return (
    <>
      <DeleteConfirm />
      {openInviteMembers && (
        <InviteMemberModal
          orgId={orgId}
          onClose={() => {
            toggleInviteMembers();
            refresh();
          }}
        />
      )}
      {openSelectClient && (
        <SelectClientModal
          onSelect={(clientId) => {
            setInviteClientId(clientId);
            toggleSelectClient();
            toggleInviteRealmUser();
          }}
          onClose={toggleSelectClient}
        />
      )}
      {openInviteRealmUser && (
        <MemberModal
          titleKey="inviteRealmUser"
          description={
            inviteClientId
              ? t("inviteRealmUserWithClient", { client: inviteClientId })
              : undefined
          }
          confirmLabelKey="send"
          filterEmptyEmail
          membersQuery={() => adminClient.organizations.listMembers({ orgId })}
          onAdd={async (selectedRows) => {
            try {
              await Promise.all(
                selectedRows.map((user) => {
                  const form = new FormData();
                  form.append("id", user.id!);
                  return adminClient.organizations.inviteExistingUser(
                    { orgId, clientId: inviteClientId },
                    form,
                  );
                }),
              );
              addAlert(
                t("organizationInvitationsSent", {
                  count: selectedRows.length,
                }),
              );
            } catch (error) {
              addError("organizationInvitationsSentError", error);
            }
          }}
          onBack={() => {
            toggleInviteRealmUser();
            toggleSelectClient();
          }}
          onClose={() => {
            toggleInviteRealmUser();
            refresh();
          }}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="invitationsList"
        onSelect={setSelectedInvitations}
        canSelectAll
        toolbarItem={
          <>
            <ToolbarItem>
              <SearchInputComponent
                value={searchText}
                onChange={setSearchText}
                onSearch={handleSearch}
                onClear={clearSearch}
                placeholder={t("searchInvitations")}
                aria-label={t("searchInvitations")}
              />
            </ToolbarItem>
            <ToolbarItem>
              <Dropdown
                onOpenChange={setIsInviteMenuOpen}
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    id="invite-member-toggle"
                    variant="primary"
                    onClick={() => setIsInviteMenuOpen(!isInviteMenuOpen)}
                    isExpanded={isInviteMenuOpen}
                  >
                    {t("inviteMember")}
                  </MenuToggle>
                )}
                isOpen={isInviteMenuOpen}
              >
                <DropdownList>
                  <DropdownItem
                    key="invite-new-user"
                    onClick={() => {
                      setIsInviteMenuOpen(false);
                      toggleInviteMembers();
                    }}
                  >
                    {t("inviteNewUser")}
                  </DropdownItem>
                  <DropdownItem
                    key="invite-realm-user"
                    onClick={() => {
                      setIsInviteMenuOpen(false);
                      toggleSelectClient();
                    }}
                  >
                    {t("inviteRealmUser")}
                  </DropdownItem>
                </DropdownList>
              </Dropdown>
            </ToolbarItem>
            <ToolbarItem>
              <Button
                icon={t("deleteInvitations")}
                variant="plain"
                isDisabled={selectedInvitations.length === 0}
                onClick={toggleDeleteDialog}
              />
            </ToolbarItem>
            <ToolbarItem>
              <CheckboxFilterComponent
                filterPlaceholderText={t("filterByStatus")}
                isOpen={isStatusFilterOpen}
                options={statusOptions}
                onOpenChange={setIsStatusFilterOpen}
                onToggleClick={() => setIsStatusFilterOpen(!isStatusFilterOpen)}
                onSelect={onStatusFilterSelect}
                selectedItems={filteredStatuses}
                width="200px"
              />
            </ToolbarItem>
          </>
        }
        actionResolver={(rowData) => {
          const invitation: OrganizationInvitationRepresentation = rowData.data;
          const actions = [
            {
              title: t("resendInvitation"),
              onClick: () => resendInvitation(invitation),
            },
            {
              title: t("deleteInvitation"),
              onClick: () => {
                setSelectedInvitations([invitation]);
                toggleDeleteDialog();
              },
            },
          ];

          return actions;
        }}
        columns={[
          {
            name: "email",
            displayKey: "email",
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellRenderer: (invitation) => invitation.firstName || "-",
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellRenderer: (invitation) => invitation.lastName || "-",
          },
          {
            name: "sentDate",
            displayKey: "sentDate",
            cellRenderer: (invitation) => (
              <DateCell date={invitation.sentDate} />
            ),
          },
          {
            name: "expiresAt",
            displayKey: "expiresAt",
            cellRenderer: (invitation) => (
              <DateCell date={invitation.expiresAt} />
            ),
          },
          {
            name: "status",
            displayKey: "status",
            cellRenderer: (invitation) => (
              <InvitationStatusBadge status={invitation.status} />
            ),
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyInvitations")}
            instructions={t("emptyInvitationsInstructions")}
            secondaryActions={[
              {
                text: t("inviteNewUser"),
                onClick: toggleInviteMembers,
              },
              {
                text: t("inviteRealmUser"),
                onClick: toggleSelectClient,
              },
            ]}
          />
        }
        isSearching={
          searchTriggerText.length > 0 || filteredStatuses.length > 0
        }
      />
    </>
  );
};
