import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  SelectControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Checkbox,
  FormGroup,
  Grid,
  GridItem,
  InputGroup,
  InputGroupItem,
  Switch,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FormAccess } from "../../components/form/FormAccess";
import { convertAttributeNameToForm } from "../../util";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { FormFields } from "../ClientDetails";
import { MultiValuedListComponent } from "../../components/dynamic/MultivaluedListComponent";
import IdentityProviderRepresentation, {
  IdentityProviderType,
} from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { useAdminClient } from "../../admin-client";
import { useState } from "react";
import { IdentityProvidersQuery } from "@keycloak/keycloak-admin-client/lib/resources/identityProviders";
import { useAccess } from "../../context/access/Access";

type CapabilityConfigProps = {
  unWrap?: boolean;
  protocol?: string;
};

export const CapabilityConfig = ({
  unWrap,
  protocol: type,
}: CapabilityConfigProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { control, watch, setValue } = useFormContext<FormFields>();
  const protocol = type || watch("protocol");
  const clientAuthentication = watch("publicClient");
  const authorization = watch("authorizationServicesEnabled");
  const jwtAuthorizationGrantEnabled = watch(
    convertAttributeNameToForm<FormFields>(
      "attributes.oauth2.jwt.authorization.grant.enabled",
    ),
    false,
  );
  const isFeatureEnabled = useIsFeatureEnabled();
  const [idps, setIdps] = useState<IdentityProviderRepresentation[]>([]);
  const [search, setSearch] = useState("");
  const { hasSomeAccess } = useAccess();
  const showIdentityProviders = hasSomeAccess("view-identity-providers");
  useFetch(
    async () => {
      if (!showIdentityProviders) {
        return [];
      }
      const params: IdentityProvidersQuery = {
        max: 20,
        realmOnly: true,
      };
      if (search) {
        params.search = search;
      }
      params.type = IdentityProviderType.JWT_AUTHORIZATION_GRANT;
      return await adminClient.identityProviders.find(params);
    },
    setIdps,
    [search],
  );
  return (
    <FormAccess
      isHorizontal
      role="manage-clients"
      unWrap={unWrap}
      className="keycloak__capability-config__form"
      data-testid="capability-config-form"
    >
      {protocol === "openid-connect" && (
        <>
          <FormGroup
            hasNoPaddingTop
            label={t("clientAuthentication")}
            fieldId="kc-authentication"
            labelIcon={
              <HelpItem
                helpText={t("authenticationHelp")}
                fieldLabelId="authentication"
              />
            }
          >
            <Controller
              name="publicClient"
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  data-testid="authentication"
                  id="kc-authentication"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={!field.value}
                  onChange={(_event, value) => {
                    field.onChange(!value);
                    if (!value) {
                      setValue("authorizationServicesEnabled", false);
                      setValue("serviceAccountsEnabled", false);
                      setValue(
                        convertAttributeNameToForm<FormFields>(
                          "attributes.oidc.ciba.grant.enabled",
                        ),
                        false,
                      );
                      setValue(
                        convertAttributeNameToForm<FormFields>(
                          "attributes.standard.token.exchange.enabled",
                        ),
                        false,
                      );
                      setValue(
                        convertAttributeNameToForm<FormFields>(
                          "attributes.oauth2.jwt.authorization.grant.enabled",
                        ),
                        false,
                      );
                    }
                  }}
                  aria-label={t("clientAuthentication")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            hasNoPaddingTop
            label={t("clientAuthorization")}
            fieldId="kc-authorization"
            labelIcon={
              <HelpItem
                helpText={t("authorizationHelp")}
                fieldLabelId="authorization"
              />
            }
          >
            <Controller
              name="authorizationServicesEnabled"
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  data-testid="authorization"
                  id="kc-authorization-switch"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value && !clientAuthentication}
                  onChange={(_event, value) => {
                    field.onChange(value);
                    if (value) {
                      setValue("serviceAccountsEnabled", true);
                    }
                  }}
                  isDisabled={clientAuthentication}
                  aria-label={t("clientAuthorization")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            hasNoPaddingTop
            label={t("authenticationFlow")}
            fieldId="kc-flow"
          >
            <Grid id="authenticationFlowGrid" hasGutter>
              <GridItem lg={4} sm={6}>
                <Controller
                  name="standardFlowEnabled"
                  defaultValue={true}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <InputGroupItem>
                        <Checkbox
                          data-testid="standard"
                          label={t("standardFlow")}
                          id="kc-flow-standard"
                          isChecked={field.value?.toString() === "true"}
                          onChange={field.onChange}
                        />
                      </InputGroupItem>
                      <InputGroupItem>
                        <HelpItem
                          helpText={t("standardFlowHelp")}
                          fieldLabelId="standardFlow"
                        />
                      </InputGroupItem>
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={8} sm={6}>
                <Controller
                  name="directAccessGrantsEnabled"
                  defaultValue={false}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <InputGroupItem>
                        <Checkbox
                          data-testid="direct"
                          label={t("directAccess")}
                          id="kc-flow-direct"
                          isChecked={field.value}
                          onChange={field.onChange}
                        />
                      </InputGroupItem>
                      <InputGroupItem>
                        <HelpItem
                          helpText={t("directAccessHelp")}
                          fieldLabelId="directAccess"
                        />
                      </InputGroupItem>
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={4} sm={6}>
                <Controller
                  name="implicitFlowEnabled"
                  defaultValue={true}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <InputGroupItem>
                        <Checkbox
                          data-testid="implicit"
                          label={t("implicitFlow")}
                          id="kc-flow-implicit"
                          isChecked={field.value?.toString() === "true"}
                          onChange={field.onChange}
                        />
                      </InputGroupItem>
                      <InputGroupItem>
                        <HelpItem
                          helpText={t("implicitFlowHelp")}
                          fieldLabelId="implicitFlow"
                        />
                      </InputGroupItem>
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={8} sm={6}>
                <Controller
                  name="serviceAccountsEnabled"
                  defaultValue={false}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <InputGroupItem>
                        <Checkbox
                          data-testid="service-account"
                          label={t("serviceAccount")}
                          id="kc-flow-service-account"
                          isChecked={
                            field.value?.toString() === "true" ||
                            (clientAuthentication && authorization)
                          }
                          onChange={field.onChange}
                          isDisabled={
                            (clientAuthentication && !authorization) ||
                            (!clientAuthentication && authorization)
                          }
                        />
                      </InputGroupItem>
                      <InputGroupItem>
                        <HelpItem
                          helpText={t("serviceAccountHelp")}
                          fieldLabelId="serviceAccount"
                        />
                      </InputGroupItem>
                    </InputGroup>
                  )}
                />
              </GridItem>
              {isFeatureEnabled(Feature.StandardTokenExchangeV2) && (
                <GridItem lg={8} sm={6}>
                  <Controller
                    name={convertAttributeNameToForm<
                      Required<ClientRepresentation["attributes"]>
                    >("attributes.standard.token.exchange.enabled")}
                    defaultValue={false}
                    control={control}
                    render={({ field }) => (
                      <InputGroup>
                        <InputGroupItem>
                          <Checkbox
                            data-testid="standard-token-exchange-enabled"
                            label={t("standardTokenExchangeEnabled")}
                            id="kc-standard-token-exchange-enabled"
                            name="standard-token-exchange-enabled"
                            isChecked={
                              field.value.toString() === "true" &&
                              !clientAuthentication
                            }
                            onChange={field.onChange}
                            isDisabled={clientAuthentication}
                          />
                        </InputGroupItem>
                        <InputGroupItem>
                          <HelpItem
                            helpText={t("standardTokenExchangeEnabledHelp")}
                            fieldLabelId="standardTokenExchangeEnabled"
                          />
                        </InputGroupItem>
                      </InputGroup>
                    )}
                  />
                </GridItem>
              )}
              {isFeatureEnabled(Feature.JWTAuthorizationGrant) && (
                <GridItem lg={8} sm={6}>
                  <Controller
                    name={convertAttributeNameToForm<
                      Required<ClientRepresentation["attributes"]>
                    >("attributes.oauth2.jwt.authorization.grant.enabled")}
                    defaultValue={false}
                    control={control}
                    render={({ field }) => (
                      <InputGroup>
                        <InputGroupItem>
                          <Checkbox
                            data-testid="jwt-authorization-grant-enabled"
                            label={t("jwtAuthorizationGrantEnabled")}
                            id="kc-jwt-authorization-grant-enabled"
                            name="jwt-authorization-grant-enabled"
                            isChecked={
                              field.value.toString() === "true" &&
                              !clientAuthentication
                            }
                            onChange={field.onChange}
                            isDisabled={clientAuthentication}
                          />
                        </InputGroupItem>
                        <InputGroupItem>
                          <HelpItem
                            helpText={t("jwtAuthorizationGrantEnabledHelp")}
                            fieldLabelId="jwtAuthorizationGrantEnabled"
                          />
                        </InputGroupItem>
                      </InputGroup>
                    )}
                  />
                </GridItem>
              )}
              {isFeatureEnabled(Feature.DeviceFlow) && (
                <GridItem lg={8} sm={6}>
                  <Controller
                    name={convertAttributeNameToForm<
                      Required<ClientRepresentation["attributes"]>
                    >("attributes.oauth2.device.authorization.grant.enabled")}
                    defaultValue={false}
                    control={control}
                    render={({ field }) => (
                      <InputGroup>
                        <InputGroupItem>
                          <Checkbox
                            data-testid="oauth-device-authorization-grant"
                            label={t("oauthDeviceAuthorizationGrant")}
                            id="kc-oauth-device-authorization-grant"
                            name="oauth2.device.authorization.grant.enabled"
                            isChecked={field.value.toString() === "true"}
                            onChange={field.onChange}
                          />
                        </InputGroupItem>
                        <InputGroupItem>
                          <HelpItem
                            helpText={t("oauthDeviceAuthorizationGrantHelp")}
                            fieldLabelId="oauthDeviceAuthorizationGrant"
                          />
                        </InputGroupItem>
                      </InputGroup>
                    )}
                  />
                </GridItem>
              )}
              <GridItem lg={8} sm={6}>
                <Controller
                  name={convertAttributeNameToForm<FormFields>(
                    "attributes.oidc.ciba.grant.enabled",
                  )}
                  defaultValue={false}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <InputGroupItem>
                        <Checkbox
                          data-testid="oidc-ciba-grant"
                          label={t("oidcCibaGrant")}
                          id="kc-oidc-ciba-grant"
                          name="oidc.ciba.grant.enabled"
                          isChecked={field.value.toString() === "true"}
                          onChange={field.onChange}
                          isDisabled={clientAuthentication}
                        />
                      </InputGroupItem>
                      <InputGroupItem>
                        <HelpItem
                          helpText={t("oidcCibaGrantHelp")}
                          fieldLabelId="oidcCibaGrant"
                        />
                      </InputGroupItem>
                    </InputGroup>
                  )}
                />
              </GridItem>
            </Grid>
          </FormGroup>
          <SelectControl
            id="keyForCodeExchange"
            label={t("keyForCodeExchange")}
            labelIcon={t("keyForCodeExchangeHelp")}
            controller={{ defaultValue: "" }}
            name={convertAttributeNameToForm<FormFields>(
              "attributes.pkce.code.challenge.method",
            )}
            options={[
              { key: "", value: t("choose") },
              { key: "S256", value: "S256" },
              { key: "plain", value: "plain" },
            ]}
          />
          {isFeatureEnabled(Feature.JWTAuthorizationGrant) &&
            showIdentityProviders &&
            jwtAuthorizationGrantEnabled.toString() === "true" && (
              <MultiValuedListComponent
                name={convertAttributeNameToForm<FormFields>(
                  "attributes.oauth2.jwt.authorization.grant.idp",
                )}
                label={t("jwtAuthorizationGrantIdp")}
                helpText={t("jwtAuthorizationGrantIdpHelp")}
                convertToName={convertAttributeNameToForm}
                stringify
                isDisabled={clientAuthentication}
                options={idps.map(({ alias }) => alias ?? "")}
                onSearch={setSearch}
              />
            )}
          {isFeatureEnabled(Feature.DPoP) && (
            <DefaultSwitchControl
              name={convertAttributeNameToForm<FormFields>(
                "attributes.dpop.bound.access.tokens",
              )}
              label={t("oAuthDPoP")}
              labelIcon={t("oAuthDPoPHelp")}
              stringify
            />
          )}
        </>
      )}
      {protocol === "saml" && (
        <>
          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.saml.encrypt",
            )}
            label={t("encryptAssertions")}
            labelIcon={t("encryptAssertionsHelp")}
          />
          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.saml.client.signature",
            )}
            label={t("clientSignature")}
            labelIcon={t("clientSignatureHelp")}
          />
        </>
      )}
    </FormAccess>
  );
};
