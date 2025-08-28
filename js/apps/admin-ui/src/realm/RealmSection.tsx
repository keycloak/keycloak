import { NetworkError } from "@keycloak/keycloak-admin-client";
import { KeycloakDataTable, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Badge,
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  PageSection,
  Popover,
  ToolbarItem,
} from "@patternfly/react-core";
import { EllipsisVIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { fetchAdminUI } from "../context/auth/admin-ui-endpoint";
import { useRealm } from "../context/realm-context/RealmContext";
import { useRecentRealms } from "../context/RecentRealms";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { translationFormatter } from "../utils/translationFormatter";
import NewRealmForm from "./add/NewRealmForm";
import { toRealm } from "./RealmRoutes";
import { toDashboard } from "../dashboard/routes/Dashboard";

export type RealmNameRepresentation = {
  name: string;
  displayName?: string;
};

const RecentRealmsDropdown = () => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const recentRealms = useRecentRealms();

  if (recentRealms.length < 3) return null;
  return (
    <Dropdown
      shouldFocusToggleOnSelect
      onOpenChange={(isOpen) => setOpen(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          data-testid="kebab"
          aria-label="Kebab toggle"
          ref={ref}
          onClick={() => setOpen(!open)}
        >
          {t("recentRealms")}
        </MenuToggle>
      )}
      isOpen={open}
    >
      <DropdownList>
        {recentRealms.map(({ name }) => (
          <DropdownItem
            key="server info"
            component={(props) => (
              <Link {...props} to={toDashboard({ realm: name })} />
            )}
          >
            {name}
          </DropdownItem>
        ))}
      </DropdownList>
    </Dropdown>
  );
};

type KebabDropdownProps = {
  onClick: () => void;
  isDisabled?: boolean;
};

const KebabDropdown = ({ onClick, isDisabled }: KebabDropdownProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <Dropdown
      shouldFocusToggleOnSelect
      onOpenChange={(isOpen) => setOpen(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          data-testid="kebab"
          aria-label="Kebab toggle"
          ref={ref}
          onClick={() => setOpen(!open)}
          variant="plain"
          isDisabled={isDisabled}
        >
          <EllipsisVIcon />
        </MenuToggle>
      )}
      isOpen={open}
    >
      <DropdownList>
        <DropdownItem
          data-testid="delete"
          onClick={() => {
            setOpen(false);
            onClick();
          }}
        >
          {t("delete")}
        </DropdownItem>
      </DropdownList>
    </Dropdown>
  );
};

type RealmRow = RealmNameRepresentation & { id: string };

export default function RealmSection() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { whoAmI } = useWhoAmI();
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [selected, setSelected] = useState<RealmRow[]>([]);
  const [openNewRealm, setOpenNewRealm] = useState(false);
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      const result = await fetchAdminUI<RealmNameRepresentation[]>(
        adminClient,
        "ui-ext/realms/names",
        { first: `${first}`, max: `${max}`, search: search || "" },
      );
      return result.map((r) => ({ ...r, id: r.name }));
    } catch (error) {
      if (error instanceof NetworkError && error.response.status < 500) {
        return [];
      }

      throw error;
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteConfirmRealm", {
      count: selected.length,
      name: selected[0]?.name,
    }),
    messageKey: "deleteConfirmRealmSetting",
    continueButtonLabel: "delete",
    onConfirm: async () => {
      try {
        if (selected.filter(({ name }) => name === "master").length > 0) {
          addAlert(t("cantDeleteMasterRealm"), AlertVariant.warning);
        }
        const filtered = selected.filter(({ name }) => name !== "master");
        if (filtered.length === 0) return;
        await Promise.all(
          filtered.map(({ name: realmName }) =>
            adminClient.realms.del({ realm: realmName }),
          ),
        );
        addAlert(t("deletedSuccessRealmSetting"));
        if (selected.filter(({ name }) => name === realm).length > 0) {
          navigate(toRealm({ realm: "master" }));
        }
        refresh();
        setSelected([]);
      } catch (error) {
        addError("deleteError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      {openNewRealm && (
        <NewRealmForm
          onClose={() => {
            setOpenNewRealm(false);
            refresh();
          }}
        />
      )}
      <ViewHeader titleKey="manageRealms" divider={false} />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <KeycloakDataTable
          key={key}
          loader={loader}
          isPaginated
          onSelect={setSelected}
          canSelectAll
          ariaLabelKey="selectRealm"
          searchPlaceholderKey="search"
          actions={[
            {
              title: t("delete"),
              onRowClick: (selected) => {
                setSelected([selected]);
                toggleDeleteDialog();
              },
            },
          ]}
          toolbarItem={
            <>
              <ToolbarItem>
                {whoAmI.createRealm && (
                  <Button
                    onClick={() => setOpenNewRealm(true)}
                    data-testid="add-realm"
                  >
                    {t("createRealm")}
                  </Button>
                )}
              </ToolbarItem>
              <ToolbarItem>
                <RecentRealmsDropdown />
              </ToolbarItem>
              <ToolbarItem>
                <KebabDropdown
                  onClick={toggleDeleteDialog}
                  isDisabled={selected.length === 0}
                />
              </ToolbarItem>
            </>
          }
          columns={[
            {
              name: "name",
              transforms: [cellWidth(20)],
              cellRenderer: ({ name }) =>
                name !== realm ? (
                  <Link to={toDashboard({ realm: name })}>{name}</Link>
                ) : (
                  <Popover
                    bodyContent={t("currentRealmExplain")}
                    triggerAction="hover"
                  >
                    <>
                      {name} <Badge isRead>{t("currentRealm")}</Badge>
                    </>
                  </Popover>
                ),
            },
            {
              name: "displayName",
              transforms: [cellWidth(80)],
              cellFormatters: [translationFormatter(t)],
            },
          ]}
        />
      </PageSection>
    </>
  );
}
