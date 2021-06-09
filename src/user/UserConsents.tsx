import React from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Chip, ChipGroup } from "@patternfly/react-core";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { cellWidth } from "@patternfly/react-table";
import _ from "lodash";
import type UserConsentRepresentation from "keycloak-admin/lib/defs/userConsentRepresentation";
import { CubesIcon } from "@patternfly/react-icons";
import moment from "moment";

export const UserConsents = () => {
  const { t } = useTranslation("roles");

  const adminClient = useAdminClient();
  const { id } = useParams<{ id: string }>();
  const alphabetize = (consentsList: UserConsentRepresentation[]) => {
    return _.sortBy(consentsList, (client) => client.clientId?.toUpperCase());
  };

  const loader = async () => {
    const getConsents = await adminClient.users.listConsents({ id });

    return alphabetize(getConsents);
  };

  const clientScopesRenderer = ({
    grantedClientScopes,
  }: UserConsentRepresentation) => {
    return (
      <ChipGroup className="kc-consents-chip-group">
        {grantedClientScopes!.map((currentChip) => (
          <Chip
            key={currentChip}
            isReadOnly
            className="kc-consents-chip"
            id="consents-chip-text"
          >
            {currentChip}
          </Chip>
        ))}
      </ChipGroup>
    );
  };

  const createdRenderer = ({ createDate }: UserConsentRepresentation) => {
    return <>{moment(createDate).format("MM/DD/YY hh:MM A")}</>;
  };

  const lastUpdatedRenderer = ({
    lastUpdatedDate,
  }: UserConsentRepresentation) => {
    return <>{moment(lastUpdatedDate).format("MM/DD/YY hh:MM A")}</>;
  };

  return (
    <>
      <KeycloakDataTable
        loader={loader}
        ariaLabelKey="roles:roleList"
        searchPlaceholderKey=" "
        columns={[
          {
            name: "clientId",
            displayKey: "clients:Client",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(20)],
          },
          {
            name: "grantedClientScopes",
            displayKey: "client-scopes:grantedClientScopes",
            cellFormatters: [emptyFormatter()],
            cellRenderer: clientScopesRenderer,
            transforms: [cellWidth(30)],
          },
          {
            name: "createdDate",
            displayKey: "clients:created",
            cellFormatters: [emptyFormatter()],
            cellRenderer: createdRenderer,
            transforms: [cellWidth(20)],
          },
          {
            name: "lastUpdatedDate",
            displayKey: "clients:lastUpdated",
            cellFormatters: [emptyFormatter()],
            cellRenderer: lastUpdatedRenderer,
            transforms: [cellWidth(20)],
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            icon={CubesIcon}
            message={t("users:noConsents")}
            instructions={t("users:noConsentsText")}
            onPrimaryAction={() => {}}
          />
        }
      />
    </>
  );
};
