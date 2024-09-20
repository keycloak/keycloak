import UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { KeycloakSelect } from "@keycloak/keycloak-ui-shared";
import {
  DropdownItem,
  PageSection,
  SelectOption,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { fetchAdminUI } from "../context/auth/admin-ui-endpoint";
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import useToggle from "../utils/useToggle";
import { RevocationModal } from "./RevocationModal";
import SessionsTable from "./SessionsTable";

import "./SessionsSection.css";

type FilterType = "ALL" | "REGULAR" | "OFFLINE";

type SessionFilterProps = {
  filterType: FilterType;
  onChange: (filterType: FilterType) => void;
};

const SessionFilter = ({ filterType, onChange }: SessionFilterProps) => {
  const { t } = useTranslation();

  const [open, toggle] = useToggle();

  return (
    <KeycloakSelect
      data-testid="filter-session-type-select"
      isOpen={open}
      onToggle={toggle}
      toggleIcon={<FilterIcon />}
      onSelect={(value) => {
        const filter = value as FilterType;
        onChange(filter);
        toggle();
      }}
      selections={filterType}
    >
      <SelectOption data-testid="all-sessions-option" value="ALL">
        {t("sessionsType.allSessions")}
      </SelectOption>
      <SelectOption data-testid="regular-sso-option" value="REGULAR">
        {t("sessionsType.regularSSO")}
      </SelectOption>
      <SelectOption data-testid="offline-option" value="OFFLINE">
        {t("sessionsType.offline")}
      </SelectOption>
    </KeycloakSelect>
  );
};

export default function SessionsSection() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const { addError } = useAlerts();
  const { realm } = useRealm();

  const [revocationModalOpen, setRevocationModalOpen] = useState(false);
  const [filterType, setFilterType] = useState<FilterType>("ALL");
  const [noSessions, setNoSessions] = useState(false);

  const handleRevocationModalToggle = () => {
    setRevocationModalOpen(!revocationModalOpen);
  };

  const loader = async (first?: number, max?: number, search?: string) => {
    const data = await fetchAdminUI<UserSessionRepresentation[]>(
      adminClient,
      "ui-ext/sessions",
      {
        first: `${first}`,
        max: `${max}`,
        type: filterType,
        search: search || "",
      },
    );
    setNoSessions(data.length === 0);
    return data;
  };

  const [toggleLogoutDialog, LogoutConfirm] = useConfirmDialog({
    titleKey: "logoutAllSessions",
    messageKey: "logoutAllDescription",
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.realms.logoutAll({ realm });
        refresh();
      } catch (error) {
        addError("logoutAllSessionsError", error);
      }
    },
  });

  return (
    <>
      <LogoutConfirm />
      <ViewHeader
        dropdownItems={[
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
        ]}
        titleKey="titleSessions"
        subKey="sessionExplain"
        helpUrl={helpUrls.sessionsUrl}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
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
          isSearching={filterType !== "ALL"}
          isPaginated
          filter={
            <SessionFilter
              filterType={filterType}
              onChange={(type) => {
                setFilterType(type);
                refresh();
              }}
            />
          }
        />
      </PageSection>
    </>
  );
}
