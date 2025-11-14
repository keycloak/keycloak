import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import IdentityProviderRepresentation, {
  IdentityProviderType,
} from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  Action,
  KeycloakDataTable,
  KeycloakSpinner,
  ListEmptyState,
  ScrollForm,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  DropdownItem,
  Form,
  PageSection,
  Tab,
  TabTitleText,
  Text,
  ToolbarItem,
} from "@patternfly/react-core";
import { useMemo, useState } from "react";
import {
  Controller,
  FormProvider,
  useForm,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { PermissionsTab } from "../../components/permission-tab/PermissionTab";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { toUpperCase } from "../../util";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { useParams } from "../../utils/useParams";
import { toIdentityProviderAddMapper } from "../routes/AddMapper";
import { toIdentityProviderEditMapper } from "../routes/EditMapper";
import {
  IdentityProviderParams,
  IdentityProviderTab,
  toIdentityProvider,
} from "../routes/IdentityProvider";
import { toIdentityProviders } from "../routes/IdentityProviders";
import { AdvancedSettings } from "./AdvancedSettings";
import { DescriptorSettings } from "./DescriptorSettings";
import { DiscoverySettings } from "./DiscoverySettings";
import { ExtendedNonDiscoverySettings } from "./ExtendedNonDiscoverySettings";
import { ExtendedOAuth2Settings } from "./ExtendedOAuth2Settings";
import { GeneralSettings } from "./GeneralSettings";
import { OIDCAuthentication } from "./OIDCAuthentication";
import { OIDCGeneralSettings } from "./OIDCGeneralSettings";
import { ReqAuthnConstraints } from "./ReqAuthnConstraintsSettings";
import { SamlGeneralSettings } from "./SamlGeneralSettings";
import { SpiffeSettings } from "./SpiffeSettings";
import { AdminEvents } from "../../events/AdminEvents";
import { UserProfileClaimsSettings } from "./OAuth2UserProfileClaimsSettings";
import { KubernetesSettings } from "./KubernetesSettings";
import { JWTAuthorizationGrantAssertionSettings } from "./JWTAuthorizationGrantAssertionSettings";
import JWTAuthorizationGrantSettings from "./JWTAuthorizationGrantSettings";
import { DefaultSwitchControl } from "../../components/SwitchControl";

type HeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean;
  save: () => void;
  toggleDeleteDialog: () => void;
};

type IdPWithMapperAttributes = IdentityProviderMapperRepresentation & {
  name: string;
  category?: string;
  helpText?: string;
  type: string;
  mapperId: string;
};

const Header = ({ onChange, value, save, toggleDeleteDialog }: HeaderProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { alias: displayName } = useParams<{ alias: string }>();
  const [provider, setProvider] = useState<IdentityProviderRepresentation>();
  const { addAlert, addError } = useAlerts();
  const { setValue, formState, control } = useFormContext();

  const validateSignature = useWatch({
    control,
    name: "config.validateSignature",
  });

  const useMetadataDescriptorUrl = useWatch({
    control,
    name: "config.useMetadataDescriptorUrl",
  });

  const metadataDescriptorUrl = useWatch({
    control,
    name: "config.metadataDescriptorUrl",
  });

  useFetch(
    () => adminClient.identityProviders.findOne({ alias: displayName }),
    (fetchedProvider) => {
      if (!fetchedProvider) {
        throw new Error(t("notFound"));
      }
      setProvider(fetchedProvider);
    },
    [],
  );

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "disableProvider",
    messageKey: t("disableConfirmIdentityProvider", { provider: displayName }),
    continueButtonLabel: "disable",
    onConfirm: () => {
      onChange(!value);
      save();
    },
  });

  const importSamlKeys = async (
    providerId: string,
    metadataDescriptorUrl: string,
  ) => {
    try {
      const result = await adminClient.identityProviders.importFromUrl({
        providerId: providerId,
        fromUrl: metadataDescriptorUrl,
      });
      if (result.signingCertificate) {
        setValue(`config.signingCertificate`, result.signingCertificate);
        addAlert(t("importKeysSuccess"), AlertVariant.success);
      } else {
        addError("importKeysError", t("importKeysErrorNoSigningCertificate"));
      }
    } catch (error) {
      addError("importKeysError", error);
    }
  };

  const reloadSamlKeys = async (alias: string) => {
    try {
      const result = await adminClient.identityProviders.reloadKeys({
        alias: alias,
      });
      if (result) {
        addAlert(t("reloadKeysSuccess"), AlertVariant.success);
      } else {
        addAlert(t("reloadKeysSuccessButFalse"), AlertVariant.warning);
      }
    } catch (error) {
      addError("reloadKeysError", error);
    }
  };

  return (
    <>
      <DisableConfirm />
      <ViewHeader
        titleKey={toUpperCase(
          provider
            ? provider.displayName
              ? provider.displayName
              : provider.providerId!
            : "",
        )}
        divider={false}
        dropdownItems={[
          ...(provider?.providerId?.includes("saml") &&
          validateSignature === "true" &&
          useMetadataDescriptorUrl === "true" &&
          metadataDescriptorUrl &&
          !formState.isDirty &&
          value
            ? [
                <DropdownItem
                  key="reloadKeys"
                  onClick={() => reloadSamlKeys(provider.alias!)}
                >
                  {t("reloadKeys")}
                </DropdownItem>,
              ]
            : provider?.providerId?.includes("saml") &&
                validateSignature === "true" &&
                useMetadataDescriptorUrl !== "true" &&
                metadataDescriptorUrl &&
                !formState.isDirty
              ? [
                  <DropdownItem
                    key="importKeys"
                    onClick={() =>
                      importSamlKeys(
                        provider.providerId!,
                        metadataDescriptorUrl,
                      )
                    }
                  >
                    {t("importKeys")}
                  </DropdownItem>,
                ]
              : []),
          <Divider key="separator" />,
          <DropdownItem key="delete" onClick={() => toggleDeleteDialog()}>
            {t("delete")}
          </DropdownItem>,
        ]}
        isEnabled={value}
        onToggle={(value) => {
          if (!value) {
            toggleDisableDialog();
          } else {
            onChange(value);
            save();
          }
        }}
      />
    </>
  );
};

type MapperLinkProps = IdPWithMapperAttributes & {
  provider?: IdentityProviderRepresentation;
};

const MapperLink = ({ name, mapperId, provider }: MapperLinkProps) => {
  const { realm } = useRealm();
  const { alias } = useParams<IdentityProviderParams>();

  return (
    <Link
      to={toIdentityProviderEditMapper({
        realm,
        alias,
        providerId: provider?.providerId!,
        id: mapperId,
      })}
    >
      {name}
    </Link>
  );
};

export default function DetailSettings() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { alias, providerId } = useParams<IdentityProviderParams>();
  const isFeatureEnabled = useIsFeatureEnabled();
  const form = useForm<IdentityProviderRepresentation>();
  const { handleSubmit, getValues, reset, control } = form;
  const [provider, setProvider] = useState<IdentityProviderRepresentation>();
  const [selectedMapper, setSelectedMapper] =
    useState<IdPWithMapperAttributes>();
  const serverInfo = useServerInfo();
  const providerInfo = useMemo(() => {
    const namespaces = [
      "org.keycloak.broker.social.SocialIdentityProvider",
      "org.keycloak.broker.provider.IdentityProvider",
    ];

    for (const namespace of namespaces) {
      const social = serverInfo.componentTypes?.[namespace]?.find(
        ({ id }) => id === providerId,
      );

      if (social) {
        return social;
      }
    }
  }, [serverInfo, providerId]);

  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm, realmRepresentation } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const { hasAccess } = useAccess();

  useFetch(
    () => adminClient.identityProviders.findOne({ alias }),
    (fetchedProvider) => {
      if (!fetchedProvider) {
        throw new Error(t("notFound"));
      }

      reset(fetchedProvider);
      setProvider(fetchedProvider);

      if (fetchedProvider.config!.authnContextClassRefs) {
        form.setValue(
          "config.authnContextClassRefs",
          JSON.parse(fetchedProvider.config?.authnContextClassRefs),
        );
      }

      if (fetchedProvider.config!.authnContextDeclRefs) {
        form.setValue(
          "config.authnContextDeclRefs",
          JSON.parse(fetchedProvider.config?.authnContextDeclRefs),
        );
      }
    },
    [],
  );

  const toTab = (tab: IdentityProviderTab) =>
    toIdentityProvider({
      realm,
      alias,
      providerId,
      tab,
    });

  const useTab = (tab: IdentityProviderTab) => useRoutableTab(toTab(tab));

  const settingsTab = useTab("settings");
  const mappersTab = useTab("mappers");
  const permissionsTab = useTab("permissions");
  const eventsTab = useTab("events");

  const save = async (savedProvider?: IdentityProviderRepresentation) => {
    const p = savedProvider || getValues();
    const origAuthnContextClassRefs = p.config?.authnContextClassRefs;
    if (p.config?.authnContextClassRefs)
      p.config.authnContextClassRefs = JSON.stringify(
        p.config.authnContextClassRefs,
      );
    const origAuthnContextDeclRefs = p.config?.authnContextDeclRefs;
    if (p.config?.authnContextDeclRefs)
      p.config.authnContextDeclRefs = JSON.stringify(
        p.config.authnContextDeclRefs,
      );

    try {
      await adminClient.identityProviders.update(
        { alias },
        {
          ...p,
          config: { ...provider?.config, ...p.config },
          alias,
          providerId,
        },
      );
      if (origAuthnContextClassRefs) {
        p.config!.authnContextClassRefs = origAuthnContextClassRefs;
      }
      if (origAuthnContextDeclRefs) {
        p.config!.authnContextDeclRefs = origAuthnContextDeclRefs;
      }
      reset(p);
      addAlert(t("updateSuccessIdentityProvider"), AlertVariant.success);
    } catch (error) {
      addError("updateErrorIdentityProvider", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteProvider",
    messageKey: t("deleteConfirmIdentityProvider", { provider: alias }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.del({ alias: alias });
        addAlert(t("deletedSuccessIdentityProvider"), AlertVariant.success);
        navigate(toIdentityProviders({ realm }));
      } catch (error) {
        addError("deleteErrorIdentityProvider", error);
      }
    },
  });

  const [toggleDeleteMapperDialog, DeleteMapperConfirm] = useConfirmDialog({
    titleKey: "deleteProviderMapper",
    messageKey: t("deleteMapperConfirm", {
      mapper: selectedMapper?.name,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.delMapper({
          alias: alias,
          id: selectedMapper?.mapperId!,
        });
        addAlert(t("deleteMapperSuccess"), AlertVariant.success);
        refresh();
        navigate(
          toIdentityProvider({ providerId, alias, tab: "mappers", realm }),
        );
      } catch (error) {
        addError("deleteErrorIdentityProvider", error);
      }
    },
  });
  const jwtAuthorizationGrantEnabled = useWatch({
    control,
    name: "config.jwtAuthorizationGrantEnabled",
  });

  if (!provider) {
    return <KeycloakSpinner />;
  }

  const isOIDC = provider.providerId!.includes("oidc");
  const isSAML = provider.providerId!.includes("saml");
  const isOAuth2 = provider.providerId!.includes("oauth2");
  const isSPIFFE = provider.providerId!.includes("spiffe");
  const isKubernetes = provider.providerId!.includes("kubernetes");
  const isJWTAuthorizationGrant = provider.providerId!.includes(
    "jwt-authorization-grant",
  );
  const isSocial = !isOIDC && !isSAML && !isOAuth2;
  const isJWTAuthorizationGrantSupported =
    (isOAuth2 || isOIDC) &&
    !!provider?.types?.includes(IdentityProviderType.JWT_AUTHORIZATION_GRANT) &&
    isFeatureEnabled(Feature.JWTAuthorizationGrant);

  const loader = async () => {
    const [loaderMappers, loaderMapperTypes] = await Promise.all([
      adminClient.identityProviders.findMappers({ alias }),
      adminClient.identityProviders.findMapperTypes({ alias }),
    ]);

    const components = loaderMappers.map((loaderMapper) => {
      const mapperType = Object.values(loaderMapperTypes).find(
        (loaderMapperType) =>
          loaderMapper.identityProviderMapper! === loaderMapperType.id!,
      );

      const result: IdPWithMapperAttributes = {
        ...mapperType,
        name: loaderMapper.name!,
        type: mapperType?.name!,
        mapperId: loaderMapper.id!,
      };

      return result;
    });

    return components;
  };

  const sections = [
    {
      title: t("generalSettings"),
      isHidden: isSPIFFE || isKubernetes || isJWTAuthorizationGrant,
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          {isSocial && <GeneralSettings create={false} id={providerId} />}
          {(isOIDC || isOAuth2) && <OIDCGeneralSettings />}
          {isSAML && <SamlGeneralSettings isAliasReadonly />}
          {providerInfo && (
            <DynamicComponents stringify properties={providerInfo.properties} />
          )}
        </FormAccess>
      ),
    },
    {
      title: t("oidcSettings"),
      isHidden: !isOIDC,
      panel: (
        <>
          <DiscoverySettings readOnly={false} isOIDC={isOIDC} />
          <Form isHorizontal className="pf-v5-u-py-lg">
            <Divider />
            <OIDCAuthentication create={false} />
          </Form>
          <ExtendedNonDiscoverySettings />
        </>
      ),
    },
    {
      title: t("oAuthSettings"),
      isHidden: !isOAuth2,
      panel: (
        <>
          <DiscoverySettings readOnly={false} isOIDC={isOIDC} />
          <Form isHorizontal className="pf-v5-u-py-lg">
            <Divider />
            <OIDCAuthentication create={false} />
          </Form>
          <UserProfileClaimsSettings />
          <ExtendedOAuth2Settings />
        </>
      ),
    },
    {
      title: t("authorizationGrantSettings"),
      isHidden: !isJWTAuthorizationGrantSupported,
      panel: (
        <>
          <Text className="pf-v5-u-pb-lg">
            {t("authorizationGrantSettingsHelp")}
          </Text>
          <Form
            isHorizontal
            className="pf-v5-u-py-lg"
            onSubmit={handleSubmit(save)}
          >
            <DefaultSwitchControl
              name="config.jwtAuthorizationGrantEnabled"
              label={t("jwtAuthorizationGrantIdpEnabled")}
              labelIcon={t("jwtAuthorizationGrantIdpEnabledHelp")}
              stringify
            />

            {jwtAuthorizationGrantEnabled === "true" && (
              <JWTAuthorizationGrantAssertionSettings />
            )}
          </Form>
        </>
      ),
    },
    {
      title: t("generalSettings"),
      isHidden: !isSPIFFE,
      panel: (
        <Form
          isHorizontal
          className="pf-v5-u-py-lg"
          onSubmit={handleSubmit(save)}
        >
          <SpiffeSettings />
          <FixedButtonsGroup name="idp-details" isSubmit reset={reset} />
        </Form>
      ),
    },
    {
      title: t("generalSettings"),
      isHidden: !isJWTAuthorizationGrant,
      panel: (
        <Form
          isHorizontal
          className="pf-v5-u-py-lg"
          onSubmit={handleSubmit(save)}
        >
          <JWTAuthorizationGrantSettings />
          <FixedButtonsGroup name="idp-details" isSubmit reset={reset} />
        </Form>
      ),
    },
    {
      title: t("generalSettings"),
      isHidden: !isKubernetes,
      panel: (
        <Form
          isHorizontal
          className="pf-v5-u-py-lg"
          onSubmit={handleSubmit(save)}
        >
          <KubernetesSettings />
          <FixedButtonsGroup name="idp-details" isSubmit reset={reset} />
        </Form>
      ),
    },
    {
      title: t("samlSettings"),
      isHidden: !isSAML,
      panel: <DescriptorSettings readOnly={false} />,
    },
    {
      title: t("reqAuthnConstraints"),
      isHidden: !isSAML,
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          <ReqAuthnConstraints />
        </FormAccess>
      ),
    },
    {
      title: t("advancedSettings"),
      isHidden: isSPIFFE || isKubernetes || isJWTAuthorizationGrant,
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          <AdvancedSettings
            isOIDC={isOIDC!}
            isSAML={isSAML!}
            isOAuth2={isOAuth2!}
          />

          <FixedButtonsGroup name="idp-details" isSubmit reset={reset} />
        </FormAccess>
      ),
    },
  ];

  return (
    <FormProvider {...form}>
      <DeleteConfirm />
      <DeleteMapperConfirm />
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ field }) => (
          <Header
            value={field.value || false}
            onChange={field.onChange}
            save={save}
            toggleDeleteDialog={toggleDeleteDialog}
          />
        )}
      />

      <PageSection variant="light" className="pf-v5-u-p-0">
        <RoutableTabs isBox defaultLocation={toTab("settings")}>
          <Tab
            id="settings"
            title={<TabTitleText>{t("settings")}</TabTitleText>}
            {...settingsTab}
          >
            <ScrollForm
              label={t("jumpToSection")}
              className="pf-v5-u-px-lg"
              sections={sections}
            />
          </Tab>
          <Tab
            id="mappers"
            isHidden={isSPIFFE || isKubernetes || isJWTAuthorizationGrant}
            data-testid="mappers-tab"
            title={<TabTitleText>{t("mappers")}</TabTitleText>}
            {...mappersTab}
          >
            <KeycloakDataTable
              emptyState={
                <ListEmptyState
                  message={t("noMappers")}
                  instructions={t("noMappersInstructions")}
                  primaryActionText={t("addMapper")}
                  onPrimaryAction={() =>
                    navigate(
                      toIdentityProviderAddMapper({
                        realm,
                        alias: alias!,
                        providerId: provider.providerId!,
                        tab: "mappers",
                      }),
                    )
                  }
                />
              }
              loader={loader}
              key={key}
              ariaLabelKey="mappersList"
              searchPlaceholderKey="searchForMapper"
              toolbarItem={
                <ToolbarItem>
                  <Button
                    id="add-mapper-button"
                    component={(props) => (
                      <Link
                        {...props}
                        to={toIdentityProviderAddMapper({
                          realm,
                          alias: alias!,
                          providerId: provider.providerId!,
                          tab: "mappers",
                        })}
                      />
                    )}
                    data-testid="addMapper"
                  >
                    {t("addMapper")}
                  </Button>
                </ToolbarItem>
              }
              columns={[
                {
                  name: "name",
                  displayKey: "name",
                  cellRenderer: (row) => (
                    <MapperLink {...row} provider={provider} />
                  ),
                },
                {
                  name: "category",
                  displayKey: "category",
                },
                {
                  name: "type",
                  displayKey: "type",
                },
              ]}
              actions={[
                {
                  title: t("delete"),
                  onRowClick: (mapper) => {
                    setSelectedMapper(mapper);
                    toggleDeleteMapperDialog();
                  },
                } as Action<IdPWithMapperAttributes>,
              ]}
            />
          </Tab>
          {isFeatureEnabled(Feature.AdminFineGrainedAuthz) && (
            <Tab
              id="permissions"
              data-testid="permissionsTab"
              title={<TabTitleText>{t("permissions")}</TabTitleText>}
              {...permissionsTab}
            >
              <PermissionsTab id={alias} type="identityProviders" />
            </Tab>
          )}
          {realmRepresentation?.adminEventsEnabled &&
            hasAccess("view-events") && (
              <Tab
                data-testid="admin-events-tab"
                title={<TabTitleText>{t("adminEvents")}</TabTitleText>}
                {...eventsTab}
              >
                <AdminEvents
                  resourcePath={`identity-provider/instances/${alias}`}
                />
              </Tab>
            )}
        </RoutableTabs>
      </PageSection>
    </FormProvider>
  );
}
