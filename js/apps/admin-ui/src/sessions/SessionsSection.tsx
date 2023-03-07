import { ClientSessionStat } from "@keycloak/keycloak-admin-client/lib/defs/clientSessionStat";
import {
  DropdownItem,
  PageSection,
  Select,
  SelectOption,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { FilterIcon } from "@patternfly/react-icons";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import { RevocationModal } from "./RevocationModal";
import SessionsTable from "./SessionsTable";

import "./SessionsSection.css";

type FilterType = "all" | "regular" | "offline";

export default function SessionsSection() {
  const { t } = useTranslation("sessions");

  const { adminClient } = useAdminClient();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const { addError } = useAlerts();
  const { realm } = useRealm();

  const [revocationModalOpen, setRevocationModalOpen] = useState(false);
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState<FilterType>("all");
  const [noSessions, setNoSessions] = useState(false);

  const handleRevocationModalToggle = () => {
    setRevocationModalOpen(!revocationModalOpen);
  };

  async function getClientSessions(clientSessionStats: ClientSessionStat[]) {
    const sessions = await Promise.all(
      clientSessionStats.map((client) =>
        adminClient.clients.listSessions({ id: client.id })
      )
    );

    return sessions.flat();
  }

  async function getOfflineSessions(clientSessionStats: ClientSessionStat[]) {
    const sessions = await Promise.all(
      clientSessionStats.map((client) =>
        adminClient.clients.listOfflineSessions({ id: client.id })
      )
    );

    return sessions.flat();
  }

  const loader = async () => {
    const clientSessionStats = await adminClient.realms.getClientSessionStats({
      realm,
    });

    const [clientSessions, offlineSessions] = await Promise.all([
      filterType !== "offline" ? getClientSessions(clientSessionStats) : [],
      filterType !== "regular" ? getOfflineSessions(clientSessionStats) : [],
    ]);

    setNoSessions(clientSessions.length === 0 && offlineSessions.length === 0);

    return [
      ...clientSessions.map((s) => ({
        type: t("sessionsType.regularSSO"),
        ...s,
      })),
      ...offlineSessions.map((s) => ({
        type: t("sessionsType.offline"),
        ...s,
      })),
    ];
  };

  const [toggleLogoutDialog, LogoutConfirm] = useConfirmDialog({
    titleKey: "sessions:logoutAllSessions",
    messageKey: "sessions:logoutAllDescription",
    continueButtonLabel: "common:confirm",
    onConfirm: async () => {
      try {
        await adminClient.realms.logoutAll({ realm });
        refresh();
      } catch (error) {
        addError("sessions:logoutAllSessionsError", error);
      }
    },
  });

  const dropdownItems = [
    <DropdownItem
      key="toggle-modal"
      data-testid="revocation"
      component="button"
      onClick={() => handleRevocationModalToggle()}
    >
      {t("revocation")}
    </DropdownItem>,
    <DropdownItem
      key="delete-role"
      data-testid="logout-all"
      component="button"
      isDisabled={noSessions}
      onClick={toggleLogoutDialog}
    >
      {t("signOutAllActiveSessions")}
    </DropdownItem>,
  ];

  return (
    <>
      <LogoutConfirm />
      <ViewHeader
        dropdownItems={dropdownItems}
        titleKey="sessions:title"
        subKey="sessions:sessionExplain"
        helpUrl={helpUrls.sessionsUrl}
      />
      <PageSection variant="light" className="pf-u-p-0">
        {revocationModalOpen && (
          <RevocationModal
            handleModalToggle={handleRevocationModalToggle}
            save={() => {
              handleRevocationModalToggle();
            }}
          />
        )}
        <SessionsTable
          key={key}
          loader={loader}
          filter={
            <Select
              data-testid="filter-session-type-select"
              isOpen={filterDropdownOpen}
              onToggle={(value) => setFilterDropdownOpen(value)}
              toggleIcon={<FilterIcon />}
              onSelect={(_, value) => {
                setFilterType(value as FilterType);
                refresh();
                setFilterDropdownOpen(false);
              }}
              selections={filterType}
            >
              <SelectOption data-testid="all-sessions-option" value="all">
                {t("sessionsType.allSessions")}
              </SelectOption>
              <SelectOption data-testid="regular-sso-option" value="regular">
                {t("sessionsType.regularSSO")}
              </SelectOption>
              <SelectOption data-testid="offline-option" value="offline">
                {t("sessionsType.offline")}
              </SelectOption>
            </Select>
          }
        />
      </PageSection>
    </>
  );
}
