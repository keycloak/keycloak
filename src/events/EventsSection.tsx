import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  PageSection,
  Tab,
  TabTitleText,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import { CheckCircleIcon, WarningTriangleIcon } from "@patternfly/react-icons";
import { cellWidth, expandable } from "@patternfly/react-table";
import type EventRepresentation from "keycloak-admin/lib/defs/eventRepresentation";
import moment from "moment";
import React, { useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { AdminEvents } from "./AdminEvents";
import "./events-section.css";

export const EventsSection = () => {
  const { t } = useTranslation("events");
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const loader = async (first?: number, max?: number, search?: string) => {
    const params = {
      first: first!,
      max: max!,
      realm,
    };
    if (search) {
      console.log("how to search?", search);
    }
    return await adminClient.realms.findEvents({ ...params });
  };

  const StatusRow = (event: EventRepresentation) => (
    <>
      {!event.error && (
        <span key={`status-${event.time}-${event.type}`}>
          <CheckCircleIcon
            color="green"
            key={`circle-${event.time}-${event.type}`}
          />{" "}
          {event.type}
        </span>
      )}
      {event.error && (
        <Tooltip
          content={event.error}
          key={`tooltip-${event.time}-${event.type}`}
        >
          <span key={`label-${event.time}-${event.type}`}>
            <WarningTriangleIcon
              color="orange"
              key={`triangle-${event.time}-${event.type}`}
            />{" "}
            {event.type}
          </span>
        </Tooltip>
      )}
    </>
  );

  const UserDetailLink = (event: EventRepresentation) => (
    <>
      <Link
        key={`link-${event.time}-${event.type}`}
        to={`/${realm}/users/${event.userId}/details`}
      >
        {event.userId}
      </Link>
    </>
  );

  const DetailCell = (event: EventRepresentation) => (
    <>
      <DescriptionList isHorizontal className="keycloak_eventsection_details">
        {Object.keys(event.details!).map((k) => (
          <DescriptionListGroup key={`detail-${event.time}-${event.type}`}>
            <DescriptionListTerm>{k}</DescriptionListTerm>
            <DescriptionListDescription>
              {event.details![k]}
            </DescriptionListDescription>
          </DescriptionListGroup>
        ))}
      </DescriptionList>
    </>
  );

  return (
    <>
      <ViewHeader
        titleKey="events:title"
        subKey={
          <Trans i18nKey="events:eventExplain">
            If you want to configure user events, Admin events or Event
            listeners, please enter
            <Link to={`/${realm}/realm-settings/events`}>
              {t("eventConfig")}
            </Link>
            page realm settings to configure.
          </Trans>
        }
        divider={false}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakTabs isBox>
          <Tab
            eventKey="userEvents"
            title={<TabTitleText>{t("userEvents")}</TabTitleText>}
          >
            <KeycloakDataTable
              key={key}
              loader={loader}
              detailColumns={[
                {
                  name: "details",
                  enabled: (event) => event.details !== undefined,
                  cellRenderer: DetailCell,
                },
              ]}
              isPaginated
              ariaLabelKey="events:title"
              searchPlaceholderKey="events:searchForEvent"
              toolbarItem={
                <>
                  <ToolbarItem>
                    <Button onClick={refresh}>{t("refresh")}</Button>
                  </ToolbarItem>
                </>
              }
              columns={[
                {
                  name: "time",
                  displayKey: "events:time",
                  cellRenderer: (row) => moment(row.time).format("LLL"),
                  cellFormatters: [expandable],
                },
                {
                  name: "userId",
                  displayKey: "events:user",
                  cellRenderer: UserDetailLink,
                },
                {
                  name: "type",
                  displayKey: "events:eventType",
                  cellRenderer: StatusRow,
                },
                {
                  name: "ipAddress",
                  displayKey: "events:ipAddress",
                  transforms: [cellWidth(10)],
                },
                {
                  name: "clientId",
                  displayKey: "events:client",
                },
              ]}
              emptyState={
                <ListEmptyState
                  message={t("emptyEvents")}
                  instructions={t("emptyEventsInstructions")}
                />
              }
            />
          </Tab>
          <Tab
            eventKey="adminEvents"
            title={<TabTitleText>{t("adminEvents")}</TabTitleText>}
          >
            <AdminEvents />
          </Tab>
        </KeycloakTabs>
      </PageSection>
    </>
  );
};
