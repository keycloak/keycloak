import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { MultiLineInput } from "../../components/multi-line-input/hook-form-v7/MultiLineInput";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import environment from "../../environment";
import { convertAttributeNameToForm } from "../../util";
import { SaveReset } from "../advanced/SaveReset";
import { FormFields } from "../ClientDetails";
import type { ClientSettingsProps } from "../ClientSettings";

export const AccessSettings = ({
  client,
  save,
  reset,
}: ClientSettingsProps) => {
  const { t } = useTranslation("clients");
  const { register, watch } = useFormContext<FormFields>();
  const { realm } = useRealm();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || client.access?.configure;

  const protocol = watch("protocol");
  const idpInitiatedSsoUrlName: string = watch(
    "attributes.saml_idp_initiated_sso_url_name"
  );

  return (
    <FormAccess
      isHorizontal
      fineGrainedAccess={client.access?.configure}
      role="manage-clients"
    >
      {!client.bearerOnly && (
        <>
          <FormGroup
            label={t("rootUrl")}
            fieldId="kc-root-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:rootURL"
                fieldLabelId="clients:rootUrl"
              />
            }
          >
            <KeycloakTextInput
              id="kc-root-url"
              type="url"
              {...register("rootUrl")}
            />
          </FormGroup>
          <FormGroup
            label={t("homeURL")}
            fieldId="kc-home-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:homeURL"
                fieldLabelId="clients:homeURL"
              />
            }
          >
            <KeycloakTextInput
              id="kc-home-url"
              type="url"
              {...register("baseUrl")}
            />
          </FormGroup>
          <FormGroup
            label={t("validRedirectUri")}
            fieldId="kc-redirect"
            labelIcon={
              <HelpItem
                helpText="clients-help:validRedirectURIs"
                fieldLabelId="clients:validRedirectUri"
              />
            }
          >
            <MultiLineInput
              name="redirectUris"
              aria-label={t("validRedirectUri")}
              addButtonLabel="clients:addRedirectUri"
            />
          </FormGroup>
          <FormGroup
            label={t("validPostLogoutRedirectUri")}
            fieldId="kc-postLogoutRedirect"
            labelIcon={
              <HelpItem
                helpText="clients-help:validPostLogoutRedirectURIs"
                fieldLabelId="clients:validPostLogoutRedirectUri"
              />
            }
          >
            <MultiLineInput
              name={convertAttributeNameToForm(
                "attributes.post.logout.redirect.uris"
              )}
              aria-label={t("validPostLogoutRedirectUri")}
              addButtonLabel="clients:addPostLogoutRedirectUri"
              stringify
            />
          </FormGroup>
          {protocol === "saml" && (
            <>
              <FormGroup
                label={t("idpInitiatedSsoUrlName")}
                fieldId="idpInitiatedSsoUrlName"
                labelIcon={
                  <HelpItem
                    helpText="clients-help:idpInitiatedSsoUrlName"
                    fieldLabelId="clients:idpInitiatedSsoUrlName"
                  />
                }
                helperText={
                  idpInitiatedSsoUrlName !== "" &&
                  t("idpInitiatedSsoUrlNameHelp", {
                    url: `${environment.authServerUrl}/realms/${realm}/protocol/saml/clients/${idpInitiatedSsoUrlName}`,
                  })
                }
              >
                <KeycloakTextInput
                  id="idpInitiatedSsoUrlName"
                  data-testid="idpInitiatedSsoUrlName"
                  {...register("attributes.saml_idp_initiated_sso_url_name")}
                />
              </FormGroup>
              <FormGroup
                label={t("idpInitiatedSsoRelayState")}
                fieldId="idpInitiatedSsoRelayState"
                labelIcon={
                  <HelpItem
                    helpText="clients-help:idpInitiatedSsoRelayState"
                    fieldLabelId="clients:idpInitiatedSsoRelayState"
                  />
                }
              >
                <KeycloakTextInput
                  id="idpInitiatedSsoRelayState"
                  data-testid="idpInitiatedSsoRelayState"
                  {...register("attributes.saml_idp_initiated_sso_relay_state")}
                />
              </FormGroup>
              <FormGroup
                label={t("masterSamlProcessingUrl")}
                fieldId="masterSamlProcessingUrl"
                labelIcon={
                  <HelpItem
                    helpText="clients-help:masterSamlProcessingUrl"
                    fieldLabelId="clients:masterSamlProcessingUrl"
                  />
                }
              >
                <KeycloakTextInput
                  id="masterSamlProcessingUrl"
                  type="url"
                  data-testid="masterSamlProcessingUrl"
                  {...register("adminUrl")}
                />
              </FormGroup>
            </>
          )}
          {protocol !== "saml" && (
            <FormGroup
              label={t("webOrigins")}
              fieldId="kc-web-origins"
              labelIcon={
                <HelpItem
                  helpText="clients-help:webOrigins"
                  fieldLabelId="clients:webOrigins"
                />
              }
            >
              <MultiLineInput
                name="webOrigins"
                aria-label={t("webOrigins")}
                addButtonLabel="clients:addWebOrigins"
              />
            </FormGroup>
          )}
        </>
      )}
      {protocol !== "saml" && (
        <FormGroup
          label={t("adminURL")}
          fieldId="kc-admin-url"
          labelIcon={
            <HelpItem
              helpText="clients-help:adminURL"
              fieldLabelId="clients:adminURL"
            />
          }
        >
          <KeycloakTextInput
            id="kc-admin-url"
            type="url"
            {...register("adminUrl")}
          />
        </FormGroup>
      )}
      {client.bearerOnly && (
        <SaveReset
          className="keycloak__form_actions"
          name="settings"
          save={save}
          reset={reset}
          isActive={!isManager}
        />
      )}
    </FormAccess>
  );
};
