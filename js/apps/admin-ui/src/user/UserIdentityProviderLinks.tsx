import type FederatedIdentityLinkRepresentation from "@keycloak/keycloak-admin-client/lib/defs/federatedIdentityLinkRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Label,
  PageSection,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { capitalize } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { FormPanel } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { toIdentityProvider } from "../identity-providers/routes/IdentityProvider";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { UserIdpModal } from "./UserIdPModal";
import { useAccess } from "../context/access/Access";

type UserIdentityProviderLinksProps = {
  userId: string;
};

export const UserIdentityProviderLinks = ({
  userId,
}: UserIdentityProviderLinksProps) => {
  const { adminClient } = useAdminClient();
  const [key, setKey] = useState(0);
  const [federatedId, setFederatedId] = useState("");
  const [isLinkIdPModalOpen, setIsLinkIdPModalOpen] = useState(false);
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const { hasAccess, hasSomeAccess } = useAccess();

  const canQueryIDPDetails = hasSomeAccess(
    "manage-identity-providers",
    "view-identity-providers",
  );

  const refresh = () => {
    setKey(new Date().getTime());
  };

  const linkedIdPsLoader = () =>
    adminClient.users.listFederatedIdentityLinks({ id: userId, linked: true });

  const availableIdPsLoader = async (
    first?: number,
    max?: number,
    search?: string,
  ) =>
    adminClient.users.listFederatedIdentityLinks({
      id: userId,
      linked: false,
      first,
      max,
      search,
    });

  const [toggleUnlinkDialog, UnlinkConfirm] = useConfirmDialog({
    titleKey: t("unlinkAccountTitle", {
      provider: capitalize(federatedId),
    }),
    messageKey: t("unlinkAccountConfirm", {
      provider: capitalize(federatedId),
    }),
    continueButtonLabel: "unlink",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: async () => {
      try {
        await adminClient.users.delFromFederatedIdentity({
          id: userId,
          federatedIdentityId: federatedId,
        });
        addAlert(t("idpUnlinkSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("mappingDeletedError", error);
      }
    },
  });

  const idpLinkRenderer = (idp: FederatedIdentityLinkRepresentation) => {
    if (!canQueryIDPDetails)
      return <span>{capitalize(idp.identityProvider)}</span>;

    return (
      <Link
        to={toIdentityProvider({
          realm,
          providerId: idp.providerId!,
          alias: idp.identityProvider!,
          tab: "settings",
        })}
      >
        {capitalize(idp.identityProvider)}
      </Link>
    );
  };

  const badgeRenderer = (idp: FederatedIdentityLinkRepresentation) => (
    <Label color={idp.social ? "blue" : "orange"}>
      {idp.social ? t("idpType.social") : t("idpType.custom")}
    </Label>
  );

  const unlinkRenderer = (fedIdentity: FederatedIdentityLinkRepresentation) => {
    if (!hasAccess("manage-users")) return <span />;

    return (
      <Button
        variant="link"
        onClick={() => {
          setFederatedId(fedIdentity.identityProvider!);
          toggleUnlinkDialog();
        }}
      >
        {t("unlinkAccount")}
      </Button>
    );
  };

  const linkRenderer = (idp: FederatedIdentityLinkRepresentation) => (
    <Button
      variant="link"
      onClick={() => {
        setFederatedId(idp.identityProvider!);
        setIsLinkIdPModalOpen(true);
      }}
    >
      {t("linkAccount")}
    </Button>
  );

  const linkedIdpColumns = () => {
    const columns = [
      {
        name: "identityProvider",
        displayKey: "name",
        cellRenderer: idpLinkRenderer,
        transforms: [cellWidth(20)],
      },
      {
        name: "userId",
        displayKey: "userID",
        cellFormatters: [emptyFormatter()],
        transforms: [cellWidth(30)],
      },
      {
        name: "userName",
        displayKey: "username",
        cellFormatters: [emptyFormatter()],
        transforms: [cellWidth(20)],
      },
      {
        name: "",
        cellRenderer: unlinkRenderer,
        transforms: [cellWidth(20)],
      },
    ];

    columns.splice(1, 0, {
      name: "type",
      displayKey: "type",
      cellRenderer: badgeRenderer,
      transforms: [cellWidth(10)],
    });

    return columns;
  };

  return (
    <>
      {isLinkIdPModalOpen && (
        <UserIdpModal
          userId={userId}
          federatedId={federatedId}
          onClose={() => setIsLinkIdPModalOpen(false)}
          onRefresh={refresh}
        />
      )}
      <UnlinkConfirm />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <FormPanel title={t("linkedIdPs")} className="kc-linked-idps">
          <TextContent>
            <Text className="kc-available-idps-text">
              {t("linkedIdPsText")}
            </Text>
          </TextContent>
          <KeycloakDataTable
            loader={linkedIdPsLoader}
            key={key}
            isPaginated={false}
            ariaLabelKey="LinkedIdPs"
            className="kc-linked-IdPs-table"
            columns={linkedIdpColumns()}
            emptyState={
              <TextContent className="kc-no-providers-text">
                <Text>{t("noProvidersLinked")}</Text>
              </TextContent>
            }
          />
        </FormPanel>
        {hasAccess("manage-users") && canQueryIDPDetails && (
          <FormPanel className="kc-available-idps" title={t("availableIdPs")}>
            <TextContent>
              <Text className="kc-available-idps-text">
                {t("availableIdPsText")}
              </Text>
            </TextContent>
            <KeycloakDataTable
              loader={availableIdPsLoader}
              key={key}
              isPaginated
              searchPlaceholderKey="searchForProvider"
              ariaLabelKey="LinkedIdPs"
              className="kc-linked-IdPs-table"
              columns={[
                {
                  name: "identityProvider",
                  displayKey: "name",
                  cellFormatters: [emptyFormatter(), upperCaseFormatter()],
                  transforms: [cellWidth(20)],
                },
                {
                  name: "type",
                  displayKey: "type",
                  cellRenderer: badgeRenderer,
                  transforms: [cellWidth(60)],
                },
                {
                  name: "",
                  cellRenderer: linkRenderer,
                },
              ]}
              emptyState={
                <TextContent className="kc-no-providers-text">
                  <Text>{t("noAvailableIdentityProviders")}</Text>
                </TextContent>
              }
            />
          </FormPanel>
        )}
      </PageSection>
    </>
  );
};
