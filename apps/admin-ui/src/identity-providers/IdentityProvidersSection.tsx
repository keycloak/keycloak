import { Fragment, useState } from "react";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import { sortBy, groupBy } from "lodash-es";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  CardTitle,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  DropdownToggle,
  Gallery,
  PageSection,
  Spinner,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextVariants,
  ToolbarItem,
} from "@patternfly/react-core";

import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useFetch, useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { upperCaseFormatter } from "../util";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ProviderIconMapper } from "./ProviderIconMapper";
import { ManageOrderDialog } from "./ManageOrderDialog";
import { toIdentityProvider } from "./routes/IdentityProvider";
import { toIdentityProviderCreate } from "./routes/IdentityProviderCreate";
import helpUrls from "../help-urls";
import { ClickableCard } from "../components/keycloak-card/ClickableCard";

export default function IdentityProvidersSection() {
  const { t } = useTranslation("identity-providers");
  const identityProviders = groupBy(
    useServerInfo().identityProviders,
    "groupName"
  );
  const { realm } = useRealm();
  const navigate = useNavigate();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [addProviderOpen, setAddProviderOpen] = useState(false);
  const [manageDisplayDialog, setManageDisplayDialog] = useState(false);
  const [providers, setProviders] =
    useState<IdentityProviderRepresentation[]>();
  const [selectedProvider, setSelectedProvider] =
    useState<IdentityProviderRepresentation>();

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  useFetch(
    async () => {
      const provider = await adminClient.realms.findOne({ realm });
      if (!provider) {
        throw new Error(t("common:notFound"));
      }
      return provider.identityProviders!;
    },
    (providers) => {
      setProviders(sortBy(providers, ["config.guiOrder", "alias"]));
    },
    [key]
  );

  const DetailLink = (identityProvider: IdentityProviderRepresentation) => (
    <Link
      key={identityProvider.providerId}
      to={toIdentityProvider({
        realm,
        providerId: identityProvider.providerId!,
        alias: identityProvider.alias!,
        tab: "settings",
      })}
    >
      {identityProvider.displayName || identityProvider.alias}
      {!identityProvider.enabled && (
        <Badge
          key={`${identityProvider.providerId}-disabled`}
          isRead
          className="pf-u-ml-sm"
        >
          {t("common:disabled")}
        </Badge>
      )}
    </Link>
  );

  const navigateToCreate = (providerId: string) =>
    navigate(
      toIdentityProviderCreate({
        realm,
        providerId,
      })
    );

  const identityProviderOptions = () =>
    Object.keys(identityProviders).map((group) => (
      <DropdownGroup key={group} label={group}>
        {sortBy(identityProviders[group], "name").map((provider) => (
          <DropdownItem
            key={provider.id}
            value={provider.id}
            component={
              <Link
                to={toIdentityProviderCreate({
                  realm,
                  providerId: provider.id,
                })}
                data-testid={provider.id}
              >
                {provider.name}
              </Link>
            }
          />
        ))}
      </DropdownGroup>
    ));

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "identity-providers:deleteProvider",
    messageKey: t("deleteConfirm", { provider: selectedProvider?.alias }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.del({
          alias: selectedProvider!.alias!,
        });
        setProviders([
          ...providers!.filter((p) => p.alias !== selectedProvider?.alias),
        ]);
        refresh();
        addAlert(t("deletedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("identity-providers:deleteError", error);
      }
    },
  });

  if (!providers) {
    return <Spinner />;
  }

  return (
    <>
      <DeleteConfirm />
      {manageDisplayDialog && (
        <ManageOrderDialog
          onClose={() => {
            setManageDisplayDialog(false);
            refresh();
          }}
          providers={providers.filter((p) => p.enabled)}
        />
      )}
      <ViewHeader
        titleKey="common:identityProviders"
        subKey="identity-providers:listExplain"
        helpUrl={helpUrls.identityProvidersUrl}
      />
      <PageSection
        variant={providers.length === 0 ? "default" : "light"}
        className={providers.length === 0 ? "" : "pf-u-p-0"}
      >
        {providers.length === 0 && (
          <>
            <TextContent>
              <Text component={TextVariants.p}>{t("getStarted")}</Text>
            </TextContent>
            {Object.keys(identityProviders).map((group) => (
              <Fragment key={group}>
                <TextContent>
                  <Text className="pf-u-mt-lg" component={TextVariants.h2}>
                    {group}:
                  </Text>
                </TextContent>
                <hr className="pf-u-mb-lg" />
                <Gallery hasGutter>
                  {sortBy(identityProviders[group], "name").map((provider) => (
                    <ClickableCard
                      key={provider.id}
                      data-testid={`${provider.id}-card`}
                      onClick={() => navigateToCreate(provider.id)}
                    >
                      <CardTitle>
                        <Split hasGutter>
                          <SplitItem>
                            <ProviderIconMapper provider={provider} />
                          </SplitItem>
                          <SplitItem isFilled>{provider.name}</SplitItem>
                        </Split>
                      </CardTitle>
                    </ClickableCard>
                  ))}
                </Gallery>
              </Fragment>
            ))}
          </>
        )}
        {providers.length !== 0 && (
          <KeycloakDataTable
            loader={providers}
            ariaLabelKey="common:identityProviders"
            searchPlaceholderKey="identity-providers:searchForProvider"
            toolbarItem={
              <>
                <ToolbarItem>
                  <Dropdown
                    data-testid="addProviderDropdown"
                    toggle={
                      <DropdownToggle
                        onToggle={() => setAddProviderOpen(!addProviderOpen)}
                        isPrimary
                      >
                        {t("addProvider")}
                      </DropdownToggle>
                    }
                    isOpen={addProviderOpen}
                    dropdownItems={identityProviderOptions()}
                  />
                </ToolbarItem>

                <ToolbarItem>
                  <Button
                    data-testid="manageDisplayOrder"
                    variant="link"
                    onClick={() => setManageDisplayDialog(true)}
                  >
                    {t("manageDisplayOrder")}
                  </Button>
                </ToolbarItem>
              </>
            }
            actions={[
              {
                title: t("common:delete"),
                onRowClick: (provider) => {
                  setSelectedProvider(provider);
                  toggleDeleteDialog();
                },
              },
            ]}
            columns={[
              {
                name: "alias",
                displayKey: "common:name",
                cellRenderer: DetailLink,
              },
              {
                name: "providerId",
                displayKey: "identity-providers:providerDetails",
                cellFormatters: [upperCaseFormatter()],
              },
            ]}
          />
        )}
      </PageSection>
    </>
  );
}
