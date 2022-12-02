import {
  Checkbox,
  FormGroup,
  Grid,
  GridItem,
  InputGroup,
  Switch,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

import "./capability-config.css";

type CapabilityConfigProps = {
  unWrap?: boolean;
  protocol?: string;
};

export const CapabilityConfig = ({
  unWrap,
  protocol: type,
}: CapabilityConfigProps) => {
  const { t } = useTranslation("clients");
  const { control, watch, setValue } = useFormContext<FormFields>();
  const protocol = type || watch("protocol");
  const clientAuthentication = watch("publicClient");
  const authorization = watch("authorizationServicesEnabled");

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
                helpText="clients-help:authentication"
                fieldLabelId="clients:authentication"
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
                  id="kc-authentication-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={!field.value}
                  onChange={(value) => {
                    field.onChange(!value);
                    if (!value) {
                      setValue("authorizationServicesEnabled", false);
                      setValue("serviceAccountsEnabled", false);
                      setValue(
                        convertAttributeNameToForm<FormFields>(
                          "attributes.oidc.ciba.grant.enabled"
                        ),
                        false
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
                helpText="clients-help:authorization"
                fieldLabelId="clients:authorization"
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
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value && !clientAuthentication}
                  onChange={(value) => {
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
            <Grid id="authenticationFlowGrid">
              <GridItem lg={4} sm={6}>
                <Controller
                  name="standardFlowEnabled"
                  defaultValue={true}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="standard"
                        label={t("standardFlow")}
                        id="kc-flow-standard"
                        isChecked={field.value?.toString() === "true"}
                        onChange={field.onChange}
                      />
                      <HelpItem
                        helpText="clients-help:standardFlow"
                        fieldLabelId="clients:standardFlow"
                      />
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={8} sm={6}>
                <Controller
                  name="directAccessGrantsEnabled"
                  defaultValue={true}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="direct"
                        label={t("directAccess")}
                        id="kc-flow-direct"
                        isChecked={field.value}
                        onChange={field.onChange}
                      />
                      <HelpItem
                        helpText="clients-help:directAccess"
                        fieldLabelId="clients:directAccess"
                      />
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
                      <Checkbox
                        data-testid="implicit"
                        label={t("implicitFlow")}
                        id="kc-flow-implicit"
                        isChecked={field.value?.toString() === "true"}
                        onChange={field.onChange}
                      />
                      <HelpItem
                        helpText="clients-help:implicitFlow"
                        fieldLabelId="clients:implicitFlow"
                      />
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
                      <HelpItem
                        helpText="clients-help:serviceAccount"
                        fieldLabelId="clients:serviceAccount"
                      />
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={8} sm={6}>
                <Controller
                  name={convertAttributeNameToForm<
                    Required<ClientRepresentation["attributes"]>
                  >("attributes.oauth2.device.authorization.grant.enabled")}
                  defaultValue={false}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="oauth-device-authorization-grant"
                        label={t("oauthDeviceAuthorizationGrant")}
                        id="kc-oauth-device-authorization-grant"
                        name="oauth2.device.authorization.grant.enabled"
                        isChecked={field.value.toString() === "true"}
                        onChange={field.onChange}
                      />
                      <HelpItem
                        helpText="clients-help:oauthDeviceAuthorizationGrant"
                        fieldLabelId="clients:oauthDeviceAuthorizationGrant"
                      />
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={8} sm={6}>
                <Controller
                  name={convertAttributeNameToForm<FormFields>(
                    "attributes.oidc.ciba.grant.enabled"
                  )}
                  defaultValue={false}
                  control={control}
                  render={({ field }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="oidc-ciba-grant"
                        label={t("oidcCibaGrant")}
                        id="kc-oidc-ciba-grant"
                        name="oidc.ciba.grant.enabled"
                        isChecked={field.value.toString() === "true"}
                        onChange={field.onChange}
                        isDisabled={clientAuthentication}
                      />
                      <HelpItem
                        helpText="clients-help:oidcCibaGrant"
                        fieldLabelId="clients:oidcCibaGrant"
                      />
                    </InputGroup>
                  )}
                />
              </GridItem>
            </Grid>
          </FormGroup>
        </>
      )}
      {protocol === "saml" && (
        <>
          <FormGroup
            labelIcon={
              <HelpItem
                helpText="clients-help:encryptAssertions"
                fieldLabelId="clients:encryptAssertions"
              />
            }
            label={t("encryptAssertions")}
            fieldId="kc-encrypt"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.saml.encrypt"
              )}
              control={control}
              defaultValue={false}
              render={({ field }) => (
                <Switch
                  data-testid="encrypt"
                  id="kc-encrypt"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                  aria-label={t("encryptAssertions")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            labelIcon={
              <HelpItem
                helpText="clients-help:clientSignature"
                fieldLabelId="clients:clientSignature"
              />
            }
            label={t("clientSignature")}
            fieldId="kc-client-signature"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.saml.client.signature"
              )}
              control={control}
              defaultValue={false}
              render={({ field }) => (
                <Switch
                  data-testid="client-signature"
                  id="kc-client-signature"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                  aria-label={t("clientSignature")}
                />
              )}
            />
          </FormGroup>
        </>
      )}
    </FormAccess>
  );
};
