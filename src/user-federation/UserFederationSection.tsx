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

import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { KeycloakCard } from "../components/keycloak-card/KeycloakCard";
import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { DatabaseIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { RealmContext } from "../context/realm-context/RealmContext";
import { useAdminClient } from "../context/auth/AdminClient";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import "./user-federation.css";

type Config = {
  enabled: string[];
};

export const UserFederationSection = () => {
  const [userFederations, setUserFederations] = useState<
    ComponentRepresentation[]
  >();
  const { addAlert } = useAlerts();
  const { t } = useTranslation("user-federation");
  const { realm } = useContext(RealmContext);
  const adminClient = useAdminClient();

  const loader = async () => {
    const testParams: { [name: string]: string | number } = {
      parentId: realm,
      type: "org.keycloak.storage.UserStorageProvider", // MF note that this is providerType in the output, but API call is still type
    };
    const userFederations = await adminClient.components.find(testParams);
    setUserFederations(userFederations);
  };

  useEffect(() => {
    loader();
  }, []);

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
        await adminClient.components.del({ id: currentCard });
        await loader();
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
            title={userFederation.name!}
            footerText={
              userFederation.providerId === "ldap" ? "LDAP" : "Kerberos"
            }
            labelText={
              (userFederation.config as Config)!.enabled[0] !== "false"
                ? `${t("common:enabled")}`
                : `${t("common:disabled")}`
            }
            labelColor={
              (userFederation.config as Config)!.enabled[0] !== "false"
                ? "blue"
                : "gray"
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
