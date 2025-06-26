import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  Action,
  KeycloakDataTable,
  ScrollForm,
  useAlerts,
  useFetch,
  KeycloakSpinner,
  HelpItem,
  ListEmptyState,
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
  ToolbarItem,
  FormGroup,
  Gallery,
  GalleryItem,
  Grid,
  GridItem,
  ClipboardCopy,
  FileUpload,
  DropEvent,
} from "@patternfly/react-core";
import { useMemo, useState, useEffect } from "react";
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
import { toKeyProvider } from "../../realm-settings/routes/KeyProvider";
import {
  createTideComponent,
  findTideComponent,
} from "../utils/SignSettingsUtil";
import { AdminEvents } from "../../events/AdminEvents";
import { UserProfileClaimsSettings } from "./OAuth2UserProfileClaimsSettings";

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
  const { handleSubmit, getValues, reset } = form;
  const [provider, setProvider] = useState<IdentityProviderRepresentation>();
  const [selectedMapper, setSelectedMapper] =
    useState<IdPWithMapperAttributes>();
  const serverInfo = useServerInfo();

  /** TIDECLOAK IMPLEMENTATION START */
  const [backgroundImage, setBackgroundImage] = useState<File | null>(null);
  const [logo, setLogo] = useState<File | null>(null);
  const [backgroundPreviewUrl, setBackgroundPreviewUrl] = useState<string>("");
  const [logoPreviewUrl, setLogoPreviewUrl] = useState<string>("");
  const [imageDeleteRequested, setImageDeleteRequested] =
    useState<boolean>(false);
  /** TIDECLOAK IMPLEMENTATION END */

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

  /** TIDECLOAK IMPLEMENTATION START */
  const currentHost = window.location.origin;

  const backgroundUrl = `${currentHost}/realms/${realm}/tide-idp-resources/images/BACKGROUND_IMAGE`;
  const logoUrl = `${currentHost}/realms/${realm}/tide-idp-resources/images/LOGO`;

  const handleFileChange =
    (type: "background" | "logo") => (event: DropEvent, file: File) => {
      if (type === "background") {
        setBackgroundImage(file);
        setBackgroundPreviewUrl(URL.createObjectURL(file));
      } else if (type === "logo") {
        setLogo(file);
        setLogoPreviewUrl(URL.createObjectURL(file));
      }
      setImageDeleteRequested(false);
    };

  const handleClear = (type: "background" | "logo") => {
    if (type === "background") {
      setBackgroundImage(null);
      setBackgroundPreviewUrl("");
    } else if (type === "logo") {
      setLogo(null);
      setLogoPreviewUrl("");
    }
    setImageDeleteRequested(true);
  };

  async function fetchImage(type: string) {
    try {
      const response = await adminClient.tideAdmin.getImageName({ type });

      if (response === null && response === "") {
        return;
      }
      // Create a new File object from the Blob
      const file = new File([], response!, { type });
      return file;
    } catch (error) {
      console.error("Failed to fetch image: ", error);
      return null;
    }
  }
  const initializeImages = async () => {
    const backgroundImageFile = await fetchImage("BACKGROUND_IMAGE");
    const logoImageFile = await fetchImage("LOGO");

    if (
      backgroundImageFile?.size !== undefined &&
      backgroundImageFile.name !== ""
    ) {
      setBackgroundImage(backgroundImageFile);
      setBackgroundPreviewUrl(backgroundUrl);
    }

    if (logoImageFile?.size !== undefined && logoImageFile.name !== "") {
      setLogo(logoImageFile);
      setLogoPreviewUrl(logoUrl);
    }
    setImageDeleteRequested(false);
  };

  useEffect(() => {
    initializeImages();
  }, []);

  const hasValue = (value: string) =>
    value !== undefined && value !== null && value !== "" ? true : false;
  const handleImageUpdate = async (image: File | null, type: string) => {
    try {
      if (image === null && imageDeleteRequested === true) {
        // delete image on server
        await adminClient.tideAdmin.deleteImage({ type });
      } else if (image !== null && image.size > 0) {
        const formData = new FormData();
        formData.append("fileData", image);
        formData.append("fileName", image.name);
        formData.append("fileType", type);
        await adminClient.tideAdmin.uploadImage(formData);
      }
    } catch (error) {
      addError("Error upload image", error);
    }
  };

  const handleReset = async () => {
    reset();
    if (providerId === "tide") {
      await adminClient.identityProviders.update(
        { alias },
        {
          ...provider,
          config: { ...provider?.config, pendingUpdateSettings: false },
          alias,
          providerId,
        },
      );
      await initializeImages();
      setImageDeleteRequested(false);
      refresh();
    }
  };

  function getSingleValue(value: string | string[]): string {
    return Array.isArray(value) ? value[0] : value;
  }

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
    [key], // TIDE IMPLEMENTATION
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
    try {
      // Check if settings needs to be re-signed
      const { LogoURL, ImageURL, backupOn, CustomAdminUIDomain } =
        provider!.config!;
      const settingsUnchanged =
        form.getValues("config.LogoURL") === LogoURL &&
        form.getValues("config.ImageURL") === ImageURL &&
        form.getValues("config.backupOn") === backupOn &&
        form.getValues("config.CustomAdminUIDomain") === CustomAdminUIDomain;

      // always get current form values for tide idp
      const p =
        providerId === "tide" ? getValues() : savedProvider || getValues();
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
      /** TIDECLOAK IMPLEMENTATION START */

      if (providerId === "tide") {
        const tideComponent = await findTideComponent(adminClient, realm);

        if (tideComponent) {
          const vvkId = getSingleValue(tideComponent!.config!.vvkId);
          const resignSettingsRequired = hasValue(vvkId) && !settingsUnchanged;
          if (resignSettingsRequired) {
            await adminClient.tideAdmin.signIdpSettings();
          }
        }

        handleImageUpdate(logo, "LOGO");
        handleImageUpdate(backgroundImage, "BACKGROUND_IMAGE");
        setImageDeleteRequested(false);
      }
      /** TIDECLOAK IMPLEMENTATION end */
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
        /** TIDECLOAK IMPLEMENTATION START */
        if (alias === "tide") {
          await adminClient.tideAdmin.deleteImage({ type: "LOGO" }); //TIDE
          await adminClient.tideAdmin.deleteImage({ type: "BACKGROUND_IMAGE" }); // TIDE
        }
        /** TIDECLOAK IMPLEMENTATION end */
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

  if (!provider) {
    return <KeycloakSpinner />;
  }

  const isOIDC = provider.providerId!.includes("oidc");
  const isSAML = provider.providerId!.includes("saml");
  const isOAuth2 = provider.providerId!.includes("oauth2");
  const isSocial = !isOIDC && !isSAML && !isOAuth2;

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

  const handleManageLicenseClick = async () => {
    try {
      const tideComponent = await findTideComponent(adminClient, realm);

      if (!tideComponent) {
        const newComponent = await createTideComponent(
          adminClient,
          realm,
          serverInfo,
        );
        const id = getSingleValue(newComponent!.id!);
        navigateToKeyProvider(id);
      } else {
        const id = getSingleValue(tideComponent!.id!);
        navigateToKeyProvider(id);
      }
    } catch (error) {
      console.error("Error handling the link click:", error);
    }
  };

  const navigateToKeyProvider = (id: string) => {
    const path = toKeyProvider({ realm, id, providerType: "tide-vendor-key" });
    path.pathname += "/license";
    navigate(path);
  };

  const sections = [
    {
      title: t("generalSettings"),
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
            <DynamicComponents
              stringify
              properties={providerInfo.properties}
              isTideProvider={providerId === "tide"}
            />
          )}
          <FormGroup
            label={t("License")}
            labelIcon={
              <HelpItem
                helpText={"Manage your licensing here"}
                fieldLabelId={"license"}
              />
            }
            fieldId="license"
          >
            <Button
              type="button"
              onClick={async (e) => {
                e.preventDefault(); // Prevent the default anchor behavior
                await handleManageLicenseClick();
              }}
            >
              Manage License
            </Button>
          </FormGroup>
          {/* TIDECLOAK IMPLEMENTATION START */}
          {providerId === "tide" && (
            <>
              <div style={{ paddingTop: "5rem" }}>
                <h1 className="pf-v5-c-title pf-m-xl">
                  Image Upload for Keycloak Hosting
                </h1>
                <p className="pf-v5-c-content ">
                  Optionally upload images to Keycloak for hosting. This is used
                  by default if not updated above.
                </p>
              </div>
              <FormGroup
                label={t("Upload Background Image")}
                labelIcon={
                  <HelpItem
                    helpText={
                      "Upload an image for Keycloak to host for you. Remember to provide the URL on Vendor sign up. IMPORTANT: this image does not get backed up"
                    }
                    fieldLabelId={"UploadBackgroundImage"}
                  />
                }
                fieldId="background-image-upload"
              >
                <Grid hasGutter>
                  <GridItem span={12}>
                    <ClipboardCopy isReadOnly>{backgroundUrl}</ClipboardCopy>
                  </GridItem>
                  <GridItem span={12}>
                    <FileUpload
                      id="background-image-upload"
                      value={backgroundImage || undefined}
                      filename={backgroundImage?.name || ""}
                      onFileInputChange={handleFileChange("background")}
                      isRequired
                      onClearClick={() => handleClear("background")}
                    />
                  </GridItem>
                  {backgroundPreviewUrl !== "" && (
                    <GridItem span={12}>
                      <Gallery hasGutter>
                        <GalleryItem>
                          <img
                            src={backgroundPreviewUrl}
                            alt="Background Image Preview"
                            style={{ maxWidth: "200px", marginTop: "10px" }}
                          />
                        </GalleryItem>
                      </Gallery>
                    </GridItem>
                  )}
                </Grid>
              </FormGroup>
              <FormGroup
                label={t("Upload Logo Image")}
                labelIcon={
                  <HelpItem
                    helpText={
                      "Upload an image for Keycloak to host for you. Remember to provide the URL on Vendor sign up. IMPORTANT: this image does not get backed up"
                    }
                    fieldLabelId={"UploadLogoImage"}
                  />
                }
                fieldId="logo-upload"
              >
                <Grid hasGutter>
                  <GridItem span={12}>
                    <ClipboardCopy isReadOnly>{logoUrl}</ClipboardCopy>
                  </GridItem>
                  <GridItem span={12}>
                    <FileUpload
                      id="logo-upload"
                      value={logo || undefined}
                      filename={logo?.name || ""}
                      onFileInputChange={handleFileChange("logo")}
                      isRequired
                      onClearClick={() => handleClear("logo")}
                    />
                  </GridItem>
                  {logoPreviewUrl !== "" && (
                    <GridItem span={12}>
                      <Gallery hasGutter>
                        <GalleryItem>
                          <img
                            src={logoPreviewUrl}
                            alt="Logo Preview"
                            style={{ maxWidth: "200px", marginTop: "10px" }}
                          />
                        </GalleryItem>
                      </Gallery>
                    </GridItem>
                  )}
                </Grid>
              </FormGroup>
            </>
          )}
          {/* TIDECLOAK IMPLEMENTATION END */}
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

          <FixedButtonsGroup name="idp-details" isSubmit reset={handleReset} />
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
