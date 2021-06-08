import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, Label, PageSection } from "@patternfly/react-core";
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
    const consents = await adminClient.users.listConsents({ id });

    return alphabetize(consents);
  };

  const [labelClicked, setLabelClicked] = useState(false);

   useEffect(() => {
    console.log(labelClicked)
  }, [labelClicked])

  const clientScopesRenderer = ({
    grantedClientScopes,
  }: UserConsentRepresentation) => {
    if (grantedClientScopes!.length <= 5 && labelClicked) {
      return <>{grantedClientScopes!.join(", ")}</>;
    } else
      return (
        <>
          {grantedClientScopes!.slice(1, 6).join(", ")}{" "}
          <Label
            color="blue"
            style={{ cursor: "pointer" }}
            variant="outline"
            onClick={() => setLabelClicked(true)}
          >
            {grantedClientScopes!.length - 5 + " more"}
          </Label>
        </>
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
