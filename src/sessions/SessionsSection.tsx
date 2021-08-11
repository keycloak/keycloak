import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import moment from "moment";
import {
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";

import type UserSessionRepresentation from "keycloak-admin/lib/defs/userSessionRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";

import "./SessionsSection.css";

const Clients = (row: UserSessionRepresentation) => {
  return (
    <>
      {Object.values(row.clients!).map((client) => (
        <Link key={client} to="" className="pf-u-mx-sm">
          {client}
        </Link>
      ))}
    </>
  );
};

export const SessionsSection = () => {
  const { t } = useTranslation("sessions");
  const adminClient = useAdminClient();
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);
  const [filterType, setFilterType] = useState(
    t("sessionsType.allSessions").toString()
  );
  const [key, setKey] = useState(0);

  const refresh = () => {
    setKey(new Date().getTime());
  };

  const loader = async () => {
    const activeClients = await adminClient.sessions.find();
    const clientSessions = (
      await Promise.all(
        activeClients.map((client) =>
          adminClient.clients.listSessions({ id: client.id })
        )
      )
    ).flat();

    const userIds = Array.from(
      new Set(clientSessions.map((session) => session.userId))
    );
    const userSessions = (
      await Promise.all(
        userIds.map((userId) => adminClient.users.listSessions({ id: userId! }))
      )
    ).flat();

    return userSessions;
  };

  return (
    <>
      <ViewHeader titleKey="sessions:title" subKey="sessions:sessionExplain" />
      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakDataTable
          key={key}
          loader={loader}
          ariaLabelKey="session:title"
          searchPlaceholderKey="sessions:searchForSession"
          searchTypeComponent={
            <Select
              data-testid="filter-session-type-select"
              isOpen={filterDropdownOpen}
              className="kc-filter-session-type-select"
              variant={SelectVariant.single}
              onToggle={(isExpanded) => setFilterDropdownOpen(isExpanded)}
              toggleIcon={<FilterIcon />}
              onSelect={(_, value) => {
                setFilterType(value.toString());
                refresh();
                setFilterDropdownOpen(false);
              }}
              selections={filterType}
            >
              <SelectOption
                data-testid="all-sessions-option"
                value={t("sessionsType.allSessions")}
                isPlaceholder
              />
              <SelectOption
                data-testid="regular-sso-option"
                value={t("sessionsType.regularSSO")}
              />
              <SelectOption
                data-testid="offline-option"
                value={t("sessionsType.offline")}
              />
              <SelectOption
                data-testid="direct-grant-option"
                value={t("sessionsType.directGrant")}
              />
              <SelectOption
                data-testid="service-account-option"
                value={t("sessionsType.serviceAccount")}
              />
            </Select>
          }
          columns={[
            {
              name: "username",
              displayKey: "sessions:subject",
            },
            {
              name: "lastAccess",
              displayKey: "sessions:lastAccess",
              cellRenderer: (row) => moment(row.lastAccess).fromNow(),
            },
            {
              name: "start",
              displayKey: "sessions:startDate",
              cellRenderer: (row) => moment(row.lastAccess).format("LLL"),
            },
            {
              name: "clients",
              displayKey: "sessions:accessedClients",
              cellRenderer: Clients,
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("emptyTitle")}
              instructions={t("emptyInstructions")}
            />
          }
        />
      </PageSection>
    </>
  );
};
