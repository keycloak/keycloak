import { useState } from "react";
import { Link } from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  KebabToggle,
  Popover,
  Text,
  TextContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import useToggle from "../utils/useToggle";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";
import { toUserFederation } from "../user-federation/routes/UserFederation";
import { useRealm } from "../context/realm-context/RealmContext";
import { GroupPickerDialog } from "../components/group/GroupPickerDialog";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useHelp } from "ui-shared";

export const DefaultsGroupsTab = () => {
  const { t } = useTranslation("realm-settings");

  const [isKebabOpen, toggleKebab] = useToggle();
  const [isGroupPickerOpen, toggleGroupPicker] = useToggle();
  const [defaultGroups, setDefaultGroups] = useState<GroupRepresentation[]>();
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);

  const [key, setKey] = useState(0);
  const [load, setLoad] = useState(0);
  const reload = () => setLoad(load + 1);

  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { enabled } = useHelp();

  useFetch(
    () => adminClient.realms.getDefaultGroups({ realm }),
    (groups) => {
      setDefaultGroups(groups);
      setKey(key + 1);
    },
    [load]
  );

  const loader = () => Promise.resolve(defaultGroups!);

  const removeGroup = async () => {
    try {
      await Promise.all(
        selectedRows.map((group) =>
          adminClient.realms.removeDefaultGroup({
            realm,
            id: group.id!,
          })
        )
      );
      addAlert(
        t("groupRemove", { count: selectedRows.length }),
        AlertVariant.success
      );
      setSelectedRows([]);
    } catch (error) {
      addError("realm-settings:groupRemoveError", error);
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
          })
        )
      );
      addAlert(
        t("defaultGroupAdded", { count: groups.length }),
        AlertVariant.success
      );
    } catch (error) {
      addError("realm-settings:defaultGroupAddedError", error);
    }
    reload();
  };

  const [toggleRemoveDialog, RemoveDialog] = useConfirmDialog({
    titleKey: t("removeConfirmTitle", { count: selectedRows.length }),
    messageKey: t("removeConfirm", { count: selectedRows.length }),
    continueButtonLabel: "common:delete",
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
            title: "realm-settings:addDefaultGroups",
            ok: "common:add",
          }}
          onConfirm={(groups) => {
            addGroups(groups || []);
            toggleGroupPicker();
          }}
          onClose={toggleGroupPicker}
        />
      )}
      {enabled && (
        <Popover
          bodyContent={
            <Trans i18nKey="realm-settings-help:defaultGroups">
              {" "}
              <Link to={toUserFederation({ realm })} />.
            </Trans>
          }
        >
          <TextContent
            className="keycloak__section_intro__help"
            style={{
              paddingLeft: "var(--pf-c-page__main-section--PaddingLeft)",
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
        ariaLabelKey="realm-settings:defaultGroups"
        searchPlaceholderKey="realm-settings:searchForGroups"
        toolbarItem={
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
                toggle={
                  <KebabToggle
                    onToggle={toggleKebab}
                    isDisabled={selectedRows!.length === 0}
                  />
                }
                isOpen={isKebabOpen}
                isPlain
                dropdownItems={[
                  <DropdownItem
                    key="action"
                    component="button"
                    onClick={() => {
                      toggleRemoveDialog();
                      toggleKebab();
                    }}
                  >
                    {t("common:remove")}
                  </DropdownItem>,
                ]}
              />
            </ToolbarItem>
          </>
        }
        actions={[
          {
            title: t("common:remove"),
            onRowClick: (group: GroupRepresentation) => {
              setSelectedRows([group]);
              toggleRemoveDialog();
              return Promise.resolve(false);
            },
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "groups:groupName",
          },
          {
            name: "path",
            displayKey: "groups:path",
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon
            message={t("noDefaultGroups")}
            instructions={
              <Trans i18nKey="realm-settings:noDefaultGroupsInstructions">
                {" "}
                <Link to={toUserFederation({ realm })} />
                Add groups...
              </Trans>
            }
            primaryActionText={t("addGroups")}
            onPrimaryAction={toggleGroupPicker}
          />
        }
      />
    </>
  );
};
