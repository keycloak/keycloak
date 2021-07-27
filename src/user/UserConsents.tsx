import React, { useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  ButtonVariant,
  Chip,
  ChipGroup,
} from "@patternfly/react-core";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { cellWidth } from "@patternfly/react-table";
import _ from "lodash";
import type UserConsentRepresentation from "keycloak-admin/lib/defs/userConsentRepresentation";
import { CubesIcon } from "@patternfly/react-icons";
import moment from "moment";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";

export const UserConsents = () => {
  const [selectedClient, setSelectedClient] =
    useState<UserConsentRepresentation>();
  const { t } = useTranslation("roles");
  const { addAlert } = useAlerts();
  const [key, setKey] = useState(0);

  const adminClient = useAdminClient();
  const { id } = useParams<{ id: string }>();
  const alphabetize = (consentsList: UserConsentRepresentation[]) => {
    return _.sortBy(consentsList, (client) => client.clientId?.toUpperCase());
  };

  const refresh = () => setKey(new Date().getTime());

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

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "users:revokeClientScopesTitle",
    messageKey: t("users:revokeClientScopes", {
      clientId: selectedClient?.clientId,
    }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.users.revokeConsent({
          id,
          clientId: selectedClient!.clientId!,
        });

        refresh();

        addAlert(t("deleteGrantsSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(t("deleteGrantsError", { error }), AlertVariant.danger);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        loader={loader}
        key={key}
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
            transforms: [cellWidth(10)],
          },
        ]}
        actions={[
          {
            title: t("users:revoke"),
            onRowClick: (client) => {
              setSelectedClient(client);
              toggleDeleteDialog();
            },
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            icon={CubesIcon}
            message={t("users:noConsents")}
            instructions={t("users:noConsentsText")}
          />
        }
      />
    </>
  );
};
