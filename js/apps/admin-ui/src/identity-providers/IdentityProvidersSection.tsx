import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import type { IdentityProvidersQuery } from "@keycloak/keycloak-admin-client/lib/resources/identityProviders";
import { IconMapper } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  CardTitle,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  DropdownList,
  Gallery,
  MenuToggle,
  PageSection,
  Split,
  SplitItem,
  Text,
  TextContent,
  TextVariants,
  ToolbarItem,
} from "@patternfly/react-core";
import { IFormatterValueType } from "@patternfly/react-table";
import { groupBy, sortBy } from "lodash-es";
import { Fragment, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ClickableCard } from "../components/keycloak-card/ClickableCard";
import {
  Action,
  KeycloakDataTable,
} from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import helpUrls from "../help-urls";
import { upperCaseFormatter } from "../util";
import { useFetch } from "../utils/useFetch";
import { ManageOrderDialog } from "./ManageOrderDialog";
import { toIdentityProvider } from "./routes/IdentityProvider";
import { toIdentityProviderCreate } from "./routes/IdentityProviderCreate";

const DetailLink = (identityProvider: IdentityProviderRepresentation) => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  return (
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
          className="pf-v5-u-ml-sm"
        >
          {t("disabled")}
        </Badge>
      )}
    </Link>
  );
};

export default function IdentityProvidersSection() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const identityProviders = groupBy(
    useServerInfo().identityProviders,
    "groupName",
  );
  const { realm } = useRealm();
  const navigate = useNavigate();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [addProviderOpen, setAddProviderOpen] = useState(false);
  const [manageDisplayDialog, setManageDisplayDialog] = useState(false);
  const [hasProviders, setHasProviders] = useState(false);
  const [selectedProvider, setSelectedProvider] =
    useState<IdentityProviderRepresentation>();
  const { addAlert, addError } = useAlerts();

  useFetch(
    async () => adminClient.identityProviders.find({ max: 1 }),
    (providers) => {
      setHasProviders(providers.length === 1);
    },
    [key],
  );

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: IdentityProvidersQuery = {
      first: first!,
      max: max!,
    };
    if (search) {
      params.search = search;
    }
    const providers = await adminClient.identityProviders.find(params);
    return sortBy(providers, "alias");
  };

  const navigateToCreate = (providerId: string) =>
    navigate(
      toIdentityProviderCreate({
        realm,
        providerId,
      }),
    );

  const identityProviderOptions = () =>
    Object.keys(identityProviders).map((group) => (
      <DropdownGroup key={group} label={group}>
        {sortBy(identityProviders[group], "name").map((provider) => (
          <DropdownItem
            key={provider.id}
            value={provider.id}
            component="a"
            data-testid={provider.id}
            onClick={() =>
              navigate(
                toIdentityProviderCreate({
                  realm,
                  providerId: provider.id,
                }),
              )
            }
          >
            {provider.name}
          </DropdownItem>
        ))}
      </DropdownGroup>
    ));

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteProvider",
    messageKey: t("deleteConfirm", { provider: selectedProvider?.alias }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.del({
          alias: selectedProvider!.alias!,
        });
        refresh();
        addAlert(t("deletedSuccessIdentityProvider"), AlertVariant.success);
      } catch (error) {
        addError("deleteErrorIdentityProvider", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      {manageDisplayDialog && (
        <ManageOrderDialog
          onClose={() => {
            setManageDisplayDialog(false);
            refresh();
          }}
        />
      )}
      <ViewHeader
        titleKey="identityProviders"
        subKey="listExplain"
        helpUrl={helpUrls.identityProvidersUrl}
      />
      <PageSection
        variant={!hasProviders ? "default" : "light"}
        className={!hasProviders ? "" : "pf-v5-u-p-0"}
      >
        {!hasProviders && (
          <>
            <TextContent>
              <Text component={TextVariants.p}>{t("getStarted")}</Text>
            </TextContent>
            {Object.keys(identityProviders).map((group) => (
              <Fragment key={group}>
                <TextContent>
                  <Text className="pf-v5-u-mt-lg" component={TextVariants.h2}>
                    {group}:
                  </Text>
                </TextContent>
                <hr className="pf-v5-u-mb-lg" />
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
                            <IconMapper icon={provider.id} />
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
        {hasProviders && (
          <KeycloakDataTable
            key={key}
            loader={loader}
            isPaginated
            ariaLabelKey="identityProviders"
            searchPlaceholderKey="searchForProvider"
            toolbarItem={
              <>
                <ToolbarItem>
                  <Dropdown
                    data-testid="addProviderDropdown"
                    onOpenChange={(isOpen) => setAddProviderOpen(isOpen)}
                    toggle={(ref) => (
                      <MenuToggle
                        ref={ref}
                        onClick={() => setAddProviderOpen(!addProviderOpen)}
                        variant="primary"
                      >
                        {t("addProvider")}
                      </MenuToggle>
                    )}
                    isOpen={addProviderOpen}
                  >
                    <DropdownList>{identityProviderOptions()}</DropdownList>
                  </Dropdown>
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
                title: t("delete"),
                onRowClick: (provider) => {
                  setSelectedProvider(provider);
                  toggleDeleteDialog();
                },
              } as Action<IdentityProviderRepresentation>,
            ]}
            columns={[
              {
                name: "alias",
                displayKey: "name",
                cellRenderer: DetailLink,
              },
              {
                name: "providerId",
                displayKey: "providerDetails",
                cellFormatters: [upperCaseFormatter()],
              },
              {
                name: "config['kc.org']",
                displayKey: "linkedOrganization",
                cellFormatters: [
                  (data?: IFormatterValueType) => {
                    return data ? "X" : "—";
                  },
                ],
              },
            ]}
          />
        )}
      </PageSection>
    </>
  );
}
