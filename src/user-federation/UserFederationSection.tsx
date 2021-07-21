import {
  AlertVariant,
  ButtonVariant,
  Card,
  CardTitle,
  DropdownItem,
  Gallery,
  GalleryItem,
  PageSection,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { DatabaseIcon } from "@patternfly/react-icons";
import type ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { KeycloakCard } from "../components/keycloak-card/KeycloakCard";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import "./user-federation.css";

export const UserFederationSection = () => {
  const [userFederations, setUserFederations] =
    useState<ComponentRepresentation[]>();
  const { addAlert } = useAlerts();
  const { t } = useTranslation("user-federation");
  const { realm } = useRealm();
  const adminClient = useAdminClient();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const { url } = useRouteMatch();
  const history = useHistory();

  useFetch(
    () => {
      const testParams: { [name: string]: string | number } = {
        parentId: realm,
        type: "org.keycloak.storage.UserStorageProvider",
      };
      return adminClient.components.find(testParams);
    },
    (userFederations) => {
      setUserFederations(userFederations);
    },
    [key]
  );

  const ufAddProviderDropdownItems = [
    <DropdownItem
      key="itemLDAP"
      onClick={() => history.push(`${url}/ldap/new`)}
    >
      LDAP
    </DropdownItem>,
    <DropdownItem
      key="itemKerberos"
      onClick={() => history.push(`${url}/kerberos/new`)}
    >
      Kerberos
    </DropdownItem>,
  ];

  const learnMoreLinkProps = {
    title: t("common:learnMore"),
    href: "https://www.keycloak.org/docs/latest/server_admin/index.html#_user-storage-federation",
  };

  let cards;

  const [currentCard, setCurrentCard] = useState("");
  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("userFedDeleteConfirmTitle"),
    messageKey: t("userFedDeleteConfirm"),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id: currentCard });
        refresh();
        addAlert(t("userFedDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addAlert(t("userFedDeleteError", { error }), AlertVariant.danger);
      }
    },
  });

  const toggleDeleteForCard = (id: string) => {
    setCurrentCard(id);
    toggleDeleteDialog();
  };

  if (userFederations) {
    cards = userFederations.map((userFederation, index) => {
      const ufCardDropdownItems = [
        <DropdownItem
          key={`${index}-cardDelete`}
          onClick={() => {
            toggleDeleteForCard(userFederation.id!);
          }}
          data-testid="card-delete"
        >
          {t("common:delete")}
        </DropdownItem>,
      ];
      return (
        <GalleryItem
          key={index}
          className="keycloak-admin--user-federation__gallery-item"
        >
          <KeycloakCard
            id={userFederation.id!}
            dropdownItems={ufCardDropdownItems}
            providerId={userFederation.providerId!}
            title={userFederation.name!}
            footerText={
              userFederation.providerId === "ldap" ? "LDAP" : "Kerberos"
            }
            labelText={
              userFederation.config!["enabled"][0] !== "false"
                ? `${t("common:enabled")}`
                : `${t("common:disabled")}`
            }
            labelColor={
              userFederation.config!["enabled"][0] !== "false" ? "blue" : "gray"
            }
          />
        </GalleryItem>
      );
    });
  }

  return (
    <>
      <ViewHeader
        titleKey="userFederation"
        subKey="user-federation:userFederationExplanation"
        subKeyLinkProps={learnMoreLinkProps}
        {...(userFederations && userFederations.length > 0
          ? {
              lowerDropdownItems: ufAddProviderDropdownItems,
              lowerDropdownMenuTitle: "user-federation:addNewProvider",
            }
          : {})}
      />
      <PageSection>
        {userFederations && userFederations.length > 0 ? (
          <>
            <DeleteConfirm />
            <Gallery hasGutter>{cards}</Gallery>
          </>
        ) : (
          <>
            <TextContent>
              <Text component={TextVariants.p}>{t("getStarted")}</Text>
            </TextContent>
            <TextContent>
              <Text className="pf-u-mt-lg" component={TextVariants.h2}>
                {t("providers")}
              </Text>
            </TextContent>
            <hr className="pf-u-mb-lg" />
            <Gallery hasGutter>
              <Card
                className="keycloak-empty-state-card"
                isHoverable
                onClick={() => history.push(`${url}/kerberos/new`)}
                data-testid="kerberos-card"
              >
                <CardTitle>
                  <Split hasGutter>
                    <SplitItem>
                      <DatabaseIcon size="lg" />
                    </SplitItem>
                    <SplitItem isFilled>{t("addKerberos")}</SplitItem>
                  </Split>
                </CardTitle>
              </Card>
              <Card
                className="keycloak-empty-state-card"
                isHoverable
                onClick={() => history.push(`${url}/ldap/new`)}
                data-testid="ldap-card"
              >
                <CardTitle>
                  <Split hasGutter>
                    <SplitItem>
                      <DatabaseIcon size="lg" />
                    </SplitItem>
                    <SplitItem isFilled>{t("addLdap")}</SplitItem>
                  </Split>
                </CardTitle>
              </Card>
            </Gallery>
          </>
        )}
      </PageSection>
    </>
  );
};
