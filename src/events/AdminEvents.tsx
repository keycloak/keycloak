import {
  Button,
  Modal,
  ModalVariant,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import {
  cellWidth,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import type AdminEventRepresentation from "keycloak-admin/lib/defs/adminEventRepresentation";
import moment from "moment";
import React, { ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";

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

const MAX_TEXT_LENGTH = 38;
const Truncate = ({
  text,
  children,
}: {
  text?: string;
  children: (text: string) => any;
}) => {
  const definedText = text || "";
  const needsTruncation = definedText.length > MAX_TEXT_LENGTH;
  const truncatedText = definedText.substr(0, MAX_TEXT_LENGTH);
  return (
    <>
      {needsTruncation && (
        <Tooltip content={text}>{children(truncatedText + "...")}</Tooltip>
      )}
      {!needsTruncation && <>{children(definedText)}</>}
    </>
  );
};

export const AdminEvents = () => {
  const { t } = useTranslation("events");
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [authEvent, setAuthEvent] = useState<AdminEventRepresentation>();
  const [representationEvent, setRepresentationEvent] =
    useState<AdminEventRepresentation>();

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

  const LinkResource = (row: AdminEventRepresentation) => (
    <>
      <Truncate text={row.resourcePath}>
        {(text) => (
          <>
            {row.resourceType !== "COMPONENT" && (
              <Link
                to={`/${realm}/${row.resourcePath}${
                  row.resourceType !== "GROUP" ? "/settings" : ""
                }`}
              >
                {text}
              </Link>
            )}
            {row.resourceType === "COMPONENT" && <span>{text}</span>}
          </>
        )}
      </Truncate>
    </>
  );

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
            cellRenderer: (row) => moment(row.time).format("LLL"),
          },
          {
            name: "resourcePath",
            displayKey: "events:resourcePath",
            cellRenderer: LinkResource,
          },
          {
            name: "resourceType",
            displayKey: "events:resourceType",
          },
          {
            name: "operationType",
            displayKey: "events:operationType",
            transforms: [cellWidth(10)],
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
