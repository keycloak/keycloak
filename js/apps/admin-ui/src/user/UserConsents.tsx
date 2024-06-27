import type UserConsentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userConsentRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  Chip,
  ChipGroup,
} from "@patternfly/react-core";
import { CubesIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { sortBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import {
  Action,
  KeycloakDataTable,
} from "../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../util";
import useFormatDate from "../utils/useFormatDate";
import { useParams } from "../utils/useParams";

export const UserConsents = () => {
  const { adminClient } = useAdminClient();

  const [selectedClient, setSelectedClient] =
    useState<UserConsentRepresentation>();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const formatDate = useFormatDate();
  const [key, setKey] = useState(0);

  const { id } = useParams<{ id: string }>();
  const alphabetize = (consentsList: UserConsentRepresentation[]) => {
    return sortBy(consentsList, (client) => client.clientId?.toUpperCase());
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
          <Chip key={currentChip} isReadOnly className="kc-consents-chip">
            {currentChip}
          </Chip>
        ))}
      </ChipGroup>
    );
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "revokeClientScopesTitle",
    messageKey: t("revokeClientScopes", {
      clientId: selectedClient?.clientId,
    }),
    continueButtonLabel: "revoke",
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
        addError("deleteGrantsError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        loader={loader}
        key={key}
        ariaLabelKey="roleList"
        searchPlaceholderKey=" "
        columns={[
          {
            name: "clientId",
            displayKey: "Client",
            cellFormatters: [emptyFormatter()],
            transforms: [cellWidth(20)],
          },
          {
            name: "grantedClientScopes",
            displayKey: "grantedClientScopes",
            cellFormatters: [emptyFormatter()],
            cellRenderer: clientScopesRenderer,
            transforms: [cellWidth(30)],
          },
          {
            name: "createDate",
            displayKey: "created",
            transforms: [cellWidth(20)],
            cellRenderer: ({ createDate }) =>
              createDate ? formatDate(new Date(createDate)) : "—",
          },
          {
            name: "lastUpdatedDate",
            displayKey: "lastUpdated",
            transforms: [cellWidth(10)],
            cellRenderer: ({ lastUpdatedDate }) =>
              lastUpdatedDate ? formatDate(new Date(lastUpdatedDate)) : "—",
          },
        ]}
        actions={[
          {
            title: t("revoke"),
            onRowClick: (client) => {
              setSelectedClient(client);
              toggleDeleteDialog();
            },
          } as Action<UserConsentRepresentation>,
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            icon={CubesIcon}
            message={t("noConsents")}
            instructions={t("noConsentsText")}
          />
        }
      />
    </>
  );
};
