import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  ButtonVariant,
  CardTitle,
  DropdownItem,
  Gallery,
  GalleryItem,
  Icon,
  PageSection,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { DatabaseIcon } from "@patternfly/react-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ClickableCard } from "../components/keycloak-card/ClickableCard";
import { KeycloakCard } from "../components/keycloak-card/KeycloakCard";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import helpUrls from "../help-urls";
import { toUpperCase } from "../util";
import { ManagePriorityDialog } from "./ManagePriorityDialog";
import { toCustomUserFederation } from "./routes/CustomUserFederation";
import { toNewCustomUserFederation } from "./routes/NewCustomUserFederation";
import { toUserFederationKerberos } from "./routes/UserFederationKerberos";
import { toUserFederationLdap } from "./routes/UserFederationLdap";

import "./user-federation.css";

export default function UserFederationSection() {
  const { adminClient } = useAdminClient();

  const [userFederations, setUserFederations] =
    useState<ComponentRepresentation[]>();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const { realm, realmRepresentation } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const navigate = useNavigate();

  const [manageDisplayDialog, setManageDisplayDialog] = useState(false);

  const providers =
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.UserStorageProvider"
    ] || [];

  useFetch(
    async () => {
      const testParams: { [name: string]: string | number } = {
        parentId: realmRepresentation!.id!,
        type: "org.keycloak.storage.UserStorageProvider",
      };
      return adminClient.components.find(testParams);
    },
    (userFederations) => {
      setUserFederations(userFederations);
    },
    [key],
  );

  const ufAddProviderDropdownItems = useMemo(
    () =>
      providers.map((p) => (
        <DropdownItem
          key={p.id}
          onClick={() =>
            navigate(toNewCustomUserFederation({ realm, providerId: p.id! }))
          }
        >
          {p.id.toUpperCase() == "LDAP"
            ? p.id.toUpperCase()
            : toUpperCase(p.id)}
        </DropdownItem>
      )),
    [],
  );

  const lowerButtonProps = {
    variant: "link",
    onClick: () => setManageDisplayDialog(true),
    lowerButtonTitle: t("managePriorities"),
  };

  let cards;

  const [currentCard, setCurrentCard] = useState("");
  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("userFedDeleteConfirmTitle"),
    messageKey: t("userFedDeleteConfirm"),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id: currentCard });
        refresh();
        addAlert(t("userFedDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("userFedDeleteError", error);
      }
    },
  });

  const toggleDeleteForCard = (id: string) => {
    setCurrentCard(id);
    toggleDeleteDialog();
  };

  const cardSorter = (card1: any, card2: any) => {
    const a = `${card1.name}`;
    const b = `${card2.name}`;
    return a < b ? -1 : 1;
  };

  const toDetails = (providerId: string, id: string) => {
    switch (providerId) {
      case "ldap":
        return toUserFederationLdap({ realm, id });
      case "kerberos":
        return toUserFederationKerberos({ realm, id });
      default:
        return toCustomUserFederation({ realm, providerId, id });
    }
  };

  if (userFederations) {
    cards = userFederations.sort(cardSorter).map((userFederation, index) => (
      <GalleryItem
        key={index}
        className="keycloak-admin--user-federation__gallery-item"
      >
        <KeycloakCard
          to={toDetails(userFederation.providerId!, userFederation.id!)}
          dropdownItems={[
            <DropdownItem
              key={`${index}-cardDelete`}
              onClick={() => {
                toggleDeleteForCard(userFederation.id!);
              }}
              data-testid="card-delete"
            >
              {t("delete")}
            </DropdownItem>,
          ]}
          title={userFederation.name!}
          footerText={toUpperCase(userFederation.providerId!)}
          labelText={
            userFederation.config?.["enabled"]?.[0] !== "false"
              ? t("enabled")
              : t("disabled")
          }
          labelColor={
            userFederation.config?.["enabled"]?.[0] !== "false"
              ? "blue"
              : "gray"
          }
        />
      </GalleryItem>
    ));
  }

  return (
    <>
      <DeleteConfirm />
      {manageDisplayDialog && userFederations && (
        <ManagePriorityDialog
          onClose={() => setManageDisplayDialog(false)}
          components={userFederations.filter((p) => p.config?.enabled)}
        />
      )}
      <ViewHeader
        titleKey="userFederation"
        subKey="userFederationExplain"
        helpUrl={helpUrls.userFederationUrl}
        {...(userFederations && userFederations.length > 0
          ? {
              lowerDropdownItems: ufAddProviderDropdownItems,
              lowerDropdownMenuTitle: "addNewProvider",
              lowerButton: lowerButtonProps,
            }
          : {})}
      />
      <PageSection>
        {userFederations && userFederations.length > 0 ? (
          <Gallery hasGutter>{cards}</Gallery>
        ) : (
          <>
            <TextContent>
              <Text component={TextVariants.p}>{t("getStarted")}</Text>
            </TextContent>
            <TextContent>
              <Text className="pf-v5-u-mt-lg" component={TextVariants.h2}>
                {t("add-providers")}
              </Text>
            </TextContent>
            <hr className="pf-v5-u-mb-lg" />
            <Gallery hasGutter>
              {providers.map((p) => (
                <ClickableCard
                  key={p.id}
                  onClick={() =>
                    navigate(
                      toNewCustomUserFederation({ realm, providerId: p.id! }),
                    )
                  }
                  data-testid={`${p.id}-card`}
                >
                  <CardTitle>
                    <Split hasGutter>
                      <SplitItem>
                        <Icon size="lg">
                          <DatabaseIcon />
                        </Icon>
                      </SplitItem>
                      <SplitItem isFilled>
                        {t("addProvider", {
                          provider: toUpperCase(p.id!),
                          count: 4,
                        })}
                      </SplitItem>
                    </Split>
                  </CardTitle>
                </ClickableCard>
              ))}
            </Gallery>
          </>
        )}
      </PageSection>
    </>
  );
}
