import React, { ReactNode, useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Modal,
  ModalVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import moment from "moment";

import { useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { RealmContext } from "../context/realm-context/RealmContext";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import AdminEventRepresentation from "keycloak-admin/lib/defs/adminEventRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";

type DisplayDialogProps = {
  titleKey: string;
  onClose: () => void;
  children: ReactNode;
};

const DisplayDialog = ({ titleKey, onClose, children }: DisplayDialogProps) => {
  const { t } = useTranslation("events");
  return (
    <Modal
      variant={ModalVariant.medium}
      title={t(titleKey)}
      isOpen={true}
      onClose={onClose}
    >
      {children}
    </Modal>
  );
};

export const AdminEvents = () => {
  const { t } = useTranslation("events");
  const adminClient = useAdminClient();
  const { realm } = useContext(RealmContext);

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);

  const [authEvent, setAuthEvent] = useState<AdminEventRepresentation>();
  const [representationEvent, setRepresentationEvent] = useState<
    AdminEventRepresentation
  >();

  const loader = async (first?: number, max?: number, search?: string) => {
    const params = {
      first: first!,
      max: max!,
      realm,
    };
    if (search) {
      console.log("how to search?", search);
    }
    return await adminClient.realms.findAdminEvents({ ...params });
  };

  return (
    <>
      {authEvent && (
        <DisplayDialog titleKey="auth" onClose={() => setAuthEvent(undefined)}>
          <Table
            aria-label="authData"
            variant={TableVariant.compact}
            cells={[t("attribute"), t("value")]}
            rows={Object.entries(authEvent.authDetails!)}
          >
            <TableHeader />
            <TableBody />
          </Table>
        </DisplayDialog>
      )}
      {representationEvent && (
        <DisplayDialog
          titleKey="representation"
          onClose={() => setRepresentationEvent(undefined)}
        >
          some json from the changed values
        </DisplayDialog>
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="events:adminEvents"
        searchPlaceholderKey="events:searchForEvent"
        toolbarItem={
          <>
            <ToolbarItem>
              <Button onClick={refresh}>{t("refresh")}</Button>
            </ToolbarItem>
          </>
        }
        actions={[
          {
            title: t("auth"),
            onRowClick: (event) => setAuthEvent(event),
          },
          {
            title: t("representation"),
            onRowClick: (event) => setRepresentationEvent(event),
          },
        ]}
        columns={[
          {
            name: "time",
            displayKey: "events:time",
            cellRenderer: (row) => moment(row.time).fromNow(),
          },
          {
            name: "resourcePath",
            displayKey: "events:resourcePath",
          },
          {
            name: "resourceType",
            displayKey: "events:resourceType",
          },
          {
            name: "operationType",
            displayKey: "events:operationType",
          },
          {
            name: "",
            displayKey: "events:user",
            cellRenderer: (event) => event.authDetails?.userId,
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyEvents")}
            instructions={t("emptyEventsInstructions")}
          />
        }
      />
    </>
  );
};
