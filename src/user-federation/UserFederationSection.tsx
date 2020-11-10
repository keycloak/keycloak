import React, { useContext, useEffect, useState } from "react";
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

import { KeycloakCard } from "../components/keycloak-card/KeycloakCard";
import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { DatabaseIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { RealmContext } from "../context/realm-context/RealmContext";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { UserFederationRepresentation } from "./model/userFederation";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import "./user-federation.css";

export const UserFederationSection = () => {
  const [userFederations, setUserFederations] = useState<
    UserFederationRepresentation[]
  >();
  const { addAlert } = useAlerts();

  const loader = async () => {
    const testParams: { [name: string]: string | number } = {
      parentId: realm,
      type: "org.keycloak.storage.UserStorageProvider", // MF note that this is providerType in the output, but API call is still type
    };
    const result = await httpClient.doGet<UserFederationRepresentation[]>(
      `/admin/realms/${realm}/components`,
      {
        params: testParams,
      }
    );
    setUserFederations(result.data);
  };

  useEffect(() => {
    loader();
  }, []);

  const { t } = useTranslation("user-federation");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);

  const ufAddProviderDropdownItems = [
    <DropdownItem key="itemLDAP">LDAP</DropdownItem>,
    <DropdownItem key="itemKerberos">Kerberos</DropdownItem>,
  ];

  const learnMoreLinkProps = {
    title: `${t("common:learnMore")}`,
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
        httpClient
          .doDelete(`/admin/realms/${realm}/components/${currentCard}`)
          .then(() => loader());
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
            toggleDeleteForCard(userFederation.id);
          }}
        >
          {t("common:delete")}
        </DropdownItem>,
      ];
      return (
        <GalleryItem key={index}>
          <KeycloakCard
            id={userFederation.id}
            dropdownItems={ufCardDropdownItems}
            title={userFederation.name}
            footerText={
              userFederation.providerId === "ldap" ? "LDAP" : "Kerberos"
            }
            labelText={
              userFederation.config.enabled
                ? `${t("common:enabled")}`
                : `${t("common:disabled")}`
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
              lowerDropdownItems: { ufAddProviderDropdownItems },
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
              <Card isHoverable>
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
