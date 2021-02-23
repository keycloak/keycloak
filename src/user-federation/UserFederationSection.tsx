import React, { useContext, useEffect, useState } from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import { useErrorHandler } from "react-error-boundary";
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

import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { KeycloakCard } from "../components/keycloak-card/KeycloakCard";
import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { DatabaseIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { RealmContext } from "../context/realm-context/RealmContext";
import { useAdminClient, asyncStateFetch } from "../context/auth/AdminClient";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";

import "./user-federation.css";

export const UserFederationSection = () => {
  const [userFederations, setUserFederations] = useState<
    ComponentRepresentation[]
  >();
  const { addAlert } = useAlerts();
  const { t } = useTranslation("user-federation");
  const { realm } = useContext(RealmContext);
  const adminClient = useAdminClient();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const handleError = useErrorHandler();
  const { url } = useRouteMatch();
  const history = useHistory();

  useEffect(() => {
    return asyncStateFetch(
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
      handleError
    );
  }, [key]);

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
    href:
      "https://www.keycloak.org/docs/latest/server_admin/index.html#_user-storage-federation",
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
          data-cy="card-delete"
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
                isHoverable
                onClick={() => history.push(`${url}/kerberos/new`)}
                data-cy="kerberos-card"
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
              <Card isHoverable>
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
