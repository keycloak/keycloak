import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  Action,
  KeycloakDataTable,
  useAlerts,
  useFetch,
  useHelp,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Popover,
  Text,
  TextContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { EllipsisVIcon, QuestionCircleIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { GroupPickerDialog } from "../components/group/GroupPickerDialog";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { toUserFederation } from "../user-federation/routes/UserFederation";
import useToggle from "../utils/useToggle";
import { useAccess } from "../context/access/Access";

export const DefaultsGroupsTab = () => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const [isKebabOpen, toggleKebab] = useToggle();
  const [isGroupPickerOpen, toggleGroupPicker] = useToggle();
  const [defaultGroups, setDefaultGroups] = useState<GroupRepresentation[]>();
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);

  const [key, setKey] = useState(0);
  const [load, setLoad] = useState(0);
  const reload = () => setLoad(load + 1);

  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { enabled } = useHelp();

  const { hasAccess } = useAccess();
  const canAddOrRemoveGroups = hasAccess("view-users", "manage-realm");

  useFetch(
    () => adminClient.realms.getDefaultGroups({ realm }),
    (groups) => {
      setDefaultGroups(groups);
      setKey(key + 1);
    },
    [load],
  );

  const loader = () => Promise.resolve(defaultGroups!);

  const removeGroup = async () => {
    try {
      await Promise.all(
        selectedRows.map((group) =>
          adminClient.realms.removeDefaultGroup({
            realm,
            id: group.id!,
          }),
        ),
      );
      addAlert(
        t("groupRemove", { count: selectedRows.length }),
        AlertVariant.success,
      );
      setSelectedRows([]);
    } catch (error) {
      addError("groupRemoveError", error);
    }
    reload();
  };

  const addGroups = async (groups: GroupRepresentation[]) => {
    try {
      await Promise.all(
        groups.map((group) =>
          adminClient.realms.addDefaultGroup({
            realm,
            id: group.id!,
          }),
        ),
      );
      addAlert(
        t("defaultGroupAdded", { count: groups.length }),
        AlertVariant.success,
      );
    } catch (error) {
      addError("defaultGroupAddedError", error);
    }
    reload();
  };

  const [toggleRemoveDialog, RemoveDialog] = useConfirmDialog({
    titleKey: t("removeConfirmTitle", { count: selectedRows.length }),
    messageKey: t("removeConfirm", { count: selectedRows.length }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: removeGroup,
  });

  if (!defaultGroups) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <RemoveDialog />
      {isGroupPickerOpen && (
        <GroupPickerDialog
          type="selectMany"
          text={{
            title: "addDefaultGroups",
            ok: "add",
          }}
          onConfirm={async (groups) => {
            await addGroups(groups || []);
            toggleGroupPicker();
          }}
          onClose={toggleGroupPicker}
        />
      )}
      {enabled && (
        <Popover
          bodyContent={
            <Trans i18nKey="defaultGroupsHelp">
              {" "}
              <Link to={toUserFederation({ realm })} />.
            </Trans>
          }
        >
          <TextContent
            className="keycloak__section_intro__help"
            style={{
              paddingLeft: "var(--pf-v5-c-page__main-section--PaddingLeft)",
            }}
          >
            <Text>
              <QuestionCircleIcon /> {t("whatIsDefaultGroups")}
            </Text>
          </TextContent>
        </Popover>
      )}
      <KeycloakDataTable
        key={key}
        canSelectAll
        onSelect={(rows) => setSelectedRows([...rows])}
        loader={loader}
        ariaLabelKey="defaultGroups"
        searchPlaceholderKey="searchForGroups"
        toolbarItem={
          canAddOrRemoveGroups && (
            <>
              <ToolbarItem>
                <Button
                  data-testid="openCreateGroupModal"
                  variant="primary"
                  onClick={toggleGroupPicker}
                >
                  {t("addGroups")}
                </Button>
              </ToolbarItem>
              <ToolbarItem>
                <Dropdown
                  onOpenChange={toggleKebab}
                  toggle={(ref) => (
                    <MenuToggle
                      ref={ref}
                      isExpanded={isKebabOpen}
                      variant="plain"
                      onClick={toggleKebab}
                      isDisabled={selectedRows!.length === 0}
                    >
                      <EllipsisVIcon />
                    </MenuToggle>
                  )}
                  isOpen={isKebabOpen}
                  shouldFocusToggleOnSelect
                >
                  <DropdownList>
                    <DropdownItem
                      key="action"
                      component="button"
                      onClick={() => {
                        toggleRemoveDialog();
                        toggleKebab();
                      }}
                    >
                      {t("remove")}
                    </DropdownItem>
                  </DropdownList>
                </Dropdown>
              </ToolbarItem>
            </>
          )
        }
        actions={
          canAddOrRemoveGroups
            ? [
                {
                  title: t("remove"),
                  onRowClick: (group) => {
                    setSelectedRows([group]);
                    toggleRemoveDialog();
                    return Promise.resolve(false);
                  },
                } as Action<GroupRepresentation>,
              ]
            : []
        }
        columns={[
          {
            name: "name",
            displayKey: "groupName",
          },
          {
            name: "path",
            displayKey: "path",
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noDefaultGroups")}
            instructions={
              <Trans i18nKey="noDefaultGroupsInstructions">
                {" "}
                <Link
                  className="pf-v5-u-font-weight-light"
                  to={toUserFederation({ realm })}
                  role="navigation"
                  aria-label={t("identityBrokeringLink")}
                />
                Add groups...
              </Trans>
            }
            primaryActionText={canAddOrRemoveGroups ? t("addGroups") : ""}
            onPrimaryAction={toggleGroupPicker}
          />
        }
      />
    </>
  );
};
