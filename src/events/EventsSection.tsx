import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Label,
  PageSection,
  Tab,
  TabTitleText,
  ToolbarItem,
} from "@patternfly/react-core";
import moment from "moment";
import EventRepresentation from "keycloak-admin/lib/defs/eventRepresentation";

import { useAdminClient } from "../context/auth/AdminClient";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { RealmContext } from "../context/realm-context/RealmContext";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { AdminEvents } from "./AdminEvents";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";

export const EventsSection = () => {
  const { t } = useTranslation("events");
  const adminClient = useAdminClient();
  const { realm } = useContext(RealmContext);

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);

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

  const StatusRow = (event: EventRepresentation) => {
    return (
      <>
        <Label color="red" icon={<InfoCircleIcon />}>
          {event.type}
        </Label>
      </>
    );
  };

  return (
    <>
      <ViewHeader titleKey="events:title" subKey="events:eventExplain" />
      <PageSection variant="light">
        <KeycloakTabs isBox>
          <Tab
            eventKey="userEvents"
            title={<TabTitleText>{t("userEvents")}</TabTitleText>}
          >
            <KeycloakDataTable
              key={key}
              loader={loader}
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
                  cellRenderer: (row) => moment(row.time).fromNow(),
                },
                {
                  name: "userId",
                  displayKey: "events:user",
                },
                {
                  name: "type",
                  displayKey: "events:eventType",
                  cellRenderer: StatusRow,
                },
                {
                  name: "ipAddress",
                  displayKey: "events:ipAddress",
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
