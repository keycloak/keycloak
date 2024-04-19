import type FederatedIdentityRepresentation from "@keycloak/keycloak-admin-client/lib/defs/federatedIdentityRepresentation";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
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
import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { toIdentityProvider } from "../identity-providers/routes/IdentityProvider";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { UserIdpModal } from "./UserIdPModal";

type UserIdentityProviderLinksProps = {
  userId: string;
};

export const UserIdentityProviderLinks = ({
  userId,
}: UserIdentityProviderLinksProps) => {
  const [key, setKey] = useState(0);
  const [federatedId, setFederatedId] = useState("");
  const [isLinkIdPModalOpen, setIsLinkIdPModalOpen] = useState(false);

  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();

  const refresh = () => setKey(new Date().getTime());

  type WithProviderId = FederatedIdentityRepresentation & {
    providerId: string;
  };

  const identityProviders = useServerInfo().identityProviders;

  const getFederatedIdentities = async () => {
    const allProviders = await adminClient.identityProviders.find();

    const allFedIds = (await adminClient.users.listFederatedIdentities({
      id: userId,
    })) as WithProviderId[];
    for (const element of allFedIds) {
      element.providerId = allProviders.find(
        (item) => item.alias === element.identityProvider,
      )?.providerId!;
    }

    return allFedIds;
  };

  const getAvailableIdPs = async () => {
    return (await adminClient.realms.findOne({ realm }))!.identityProviders;
  };

  const linkedIdPsLoader = async () => {
    return getFederatedIdentities();
  };

  const availableIdPsLoader = async () => {
    const linkedNames = (await getFederatedIdentities()).map(
      (x) => x.identityProvider,
    );

    return (await getAvailableIdPs())?.filter(
      (item) => !linkedNames.includes(item.alias),
    )!;
  };

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

  const idpLinkRenderer = (idp: WithProviderId) => {
    return (
      <Link
        to={toIdentityProvider({
          realm,
          providerId: idp.providerId,
          alias: idp.identityProvider!,
          tab: "settings",
        })}
      >
        {capitalize(idp.identityProvider)}
      </Link>
    );
  };

  const badgeRenderer1 = (idp: FederatedIdentityRepresentation) => {
    const groupName = identityProviders?.find(
      (provider) => provider["id"] === idp.identityProvider,
    )?.groupName!;
    return (
      <Label color={groupName === "Social" ? "blue" : "orange"}>
        {groupName === "Social" ? t("idpType.social") : t("idpType.custom")}
      </Label>
    );
  };

  const badgeRenderer2 = (idp: IdentityProviderRepresentation) => {
    const groupName = identityProviders?.find(
      (provider) => provider["id"] === idp.providerId,
    )?.groupName!;
    return (
      <Label color={groupName === "User-defined" ? "orange" : "blue"}>
        {groupName === "User-defined"
          ? "Custom"
          : groupName! === "Social"
            ? t("idpType.social")
            : groupName!}
      </Label>
    );
  };

  const unlinkRenderer = (fedIdentity: FederatedIdentityRepresentation) => {
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

  const linkRenderer = (idp: IdentityProviderRepresentation) => {
    return (
      <Button
        variant="link"
        onClick={() => {
          setFederatedId(idp.alias!);
          setIsLinkIdPModalOpen(true);
        }}
      >
        {t("linkAccount")}
      </Button>
    );
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
            columns={[
              {
                name: "identityProvider",
                displayKey: "name",
                cellFormatters: [emptyFormatter()],
                cellRenderer: idpLinkRenderer,
                transforms: [cellWidth(20)],
              },
              {
                name: "type",
                displayKey: "type",
                cellFormatters: [emptyFormatter()],
                cellRenderer: badgeRenderer1,
                transforms: [cellWidth(10)],
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
                cellFormatters: [emptyFormatter()],
                cellRenderer: unlinkRenderer,
                transforms: [cellWidth(20)],
              },
            ]}
            emptyState={
              <TextContent className="kc-no-providers-text">
                <Text>{t("noProvidersLinked")}</Text>
              </TextContent>
            }
          />
        </FormPanel>
        <FormPanel className="kc-available-idps" title={t("availableIdPs")}>
          <TextContent>
            <Text className="kc-available-idps-text">
              {t("availableIdPsText")}
            </Text>
          </TextContent>
          <KeycloakDataTable
            loader={availableIdPsLoader}
            key={key}
            isPaginated={false}
            ariaLabelKey="LinkedIdPs"
            className="kc-linked-IdPs-table"
            columns={[
              {
                name: "alias",
                displayKey: "name",
                cellFormatters: [emptyFormatter(), upperCaseFormatter()],
                transforms: [cellWidth(20)],
              },
              {
                name: "type",
                displayKey: "type",
                cellFormatters: [emptyFormatter()],
                cellRenderer: badgeRenderer2,
                transforms: [cellWidth(60)],
              },
              {
                name: "",
                cellFormatters: [emptyFormatter()],
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
      </PageSection>
    </>
  );
};
