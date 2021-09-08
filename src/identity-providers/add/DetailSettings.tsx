import React from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Controller, FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  DropdownItem,
  Form,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";

import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { ScrollForm } from "../../components/scroll-form/ScrollForm";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useFetch, useAdminClient } from "../../context/auth/AdminClient";
import { GeneralSettings } from "./GeneralSettings";
import { AdvancedSettings } from "./AdvancedSettings";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../../components/alert/Alerts";
import { useRealm } from "../../context/realm-context/RealmContext";
import { KeycloakTabs } from "../../components/keycloak-tabs/KeycloakTabs";
import { ExtendedNonDiscoverySettings } from "./ExtendedNonDiscoverySettings";
import { DiscoverySettings } from "./DiscoverySettings";
import { DescriptorSettings } from "./DescriptorSettings";
import { OIDCGeneralSettings } from "./OIDCGeneralSettings";
import { SamlGeneralSettings } from "./SamlGeneralSettings";
import { OIDCAuthentication } from "./OIDCAuthentication";
import { ReqAuthnConstraints } from "./ReqAuthnConstraintsSettings";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";

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
};

const Header = ({ onChange, value, save, toggleDeleteDialog }: HeaderProps) => {
  const { t } = useTranslation("identity-providers");
  const { providerId, alias } =
    useParams<{ providerId: string; alias: string }>();

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "identity-providers:disableProvider",
    messageKey: t("disableConfirm", { provider: providerId }),
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange(!value);
      save();
    },
  });

  return (
    <>
      <DisableConfirm />
      <ViewHeader
        titleKey={alias}
        divider={false}
        dropdownItems={[
          <DropdownItem key="delete" onClick={() => toggleDeleteDialog()}>
            {t("common:delete")}
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

export const DetailSettings = () => {
  const { t } = useTranslation("identity-providers");
  const { providerId, alias } =
    useParams<{ providerId: string; alias: string }>();

  const form = useForm<IdentityProviderRepresentation>();
  const { handleSubmit, getValues, reset } = form;

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const history = useHistory();
  const { realm } = useRealm();

  useFetch(
    () => adminClient.identityProviders.findOne({ alias: alias }),
    (fetchedProvider) => {
      reset(fetchedProvider);
    },
    []
  );

  const save = async (provider?: IdentityProviderRepresentation) => {
    const p = provider || getValues();
    try {
      await adminClient.identityProviders.update(
        { alias },
        { ...p, alias, providerId }
      );
      addAlert(t("updateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("identity-providers:updateError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "identity-providers:deleteProvider",
    messageKey: t("identity-providers:deleteConfirm", { provider: providerId }),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.del({ alias: alias });
        addAlert(t("deletedSuccess"), AlertVariant.success);
        history.push(`/${realm}/identity-providers`);
      } catch (error) {
        addError("identity-providers:deleteErrorError", error);
      }
    },
  });

  const sections = [t("generalSettings"), t("advancedSettings")];

  const isOIDC = providerId.includes("oidc");
  const isSAML = providerId.includes("saml");

  const loader = async () => {
    const [loaderMappers, loaderMapperTypes] = await Promise.all([
      adminClient.identityProviders.findMappers({ alias }),
      adminClient.identityProviders.findMapperTypes({ alias }),
    ]);

    const components = loaderMappers.map((loaderMapper) => {
      const mapperType = Object.values(loaderMapperTypes).find(
        (loaderMapperType) =>
          loaderMapper.identityProviderMapper! === loaderMapperType.id!
      );

      const result: IdPWithMapperAttributes = {
        ...mapperType,
        name: loaderMapper.name!,
        type: mapperType?.name!,
      };

      return result;
    });

    return components;
  };

  if (isOIDC) {
    sections.splice(1, 0, t("oidcSettings"));
  }

  if (isSAML) {
    sections.splice(1, 0, t("samlSettings"));
    sections.splice(2, 0, t("reqAuthnConstraints"));
  }

  return (
    <>
      <DeleteConfirm />
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ onChange, value }) => (
          <Header
            value={value}
            onChange={onChange}
            save={save}
            toggleDeleteDialog={toggleDeleteDialog}
          />
        )}
      />

      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...form}>
          <KeycloakTabs isBox>
            <Tab
              id="settings"
              eventKey="settings"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            >
              <ScrollForm className="pf-u-px-lg" sections={sections}>
                <FormAccess
                  role="manage-identity-providers"
                  isHorizontal
                  onSubmit={handleSubmit(save)}
                >
                  {!isOIDC && !isSAML && (
                    <GeneralSettings create={false} id={alias} />
                  )}
                  {isOIDC && <OIDCGeneralSettings id={alias} />}
                  {isSAML && <SamlGeneralSettings id={alias} />}
                </FormAccess>
                {isOIDC && (
                  <>
                    <DiscoverySettings readOnly={false} />
                    <Form isHorizontal className="pf-u-py-lg">
                      <Divider />
                      <OIDCAuthentication create={false} />
                    </Form>
                    <ExtendedNonDiscoverySettings />
                  </>
                )}
                {isSAML && <DescriptorSettings readOnly={false} />}
                {isSAML && (
                  <FormAccess
                    role="manage-identity-providers"
                    isHorizontal
                    onSubmit={handleSubmit(save)}
                  >
                    <ReqAuthnConstraints />
                  </FormAccess>
                )}
                <FormAccess
                  role="manage-identity-providers"
                  isHorizontal
                  onSubmit={handleSubmit(save)}
                >
                  <AdvancedSettings isOIDC={isOIDC} isSAML={isSAML} />

                  <ActionGroup className="keycloak__form_actions">
                    <Button data-testid={"save"} type="submit">
                      {t("common:save")}
                    </Button>
                    <Button
                      data-testid={"revert"}
                      variant="link"
                      onClick={() => {
                        reset();
                      }}
                    >
                      {t("common:revert")}
                    </Button>
                  </ActionGroup>
                </FormAccess>
              </ScrollForm>
            </Tab>
            <Tab
              id="mappers"
              eventKey="mappers"
              title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
            >
              <KeycloakDataTable
                emptyState={
                  <ListEmptyState
                    message={t("identity-providers:noMappers")}
                    instructions={t("identity-providers:noMappersInstructions")}
                    primaryActionText={t("identity-providers:addMapper")}
                  />
                }
                loader={loader}
                isPaginated
                ariaLabelKey="identity-providers:mappersList"
                searchPlaceholderKey="identity-providers:searchForMapper"
                columns={[
                  {
                    name: "name",
                    displayKey: "common:name",
                  },
                  {
                    name: "category",
                    displayKey: "common:category",
                  },
                  {
                    name: "type",
                    displayKey: "common:type",
                  },
                ]}
              />
            </Tab>
          </KeycloakTabs>
        </FormProvider>
      </PageSection>
    </>
  );
};
