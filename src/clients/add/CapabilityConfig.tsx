import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Switch,
  Checkbox,
  Grid,
  GridItem,
  InputGroup,
} from "@patternfly/react-core";

import { FormAccess } from "../../components/form-access/FormAccess";
import type { ClientForm } from "../ClientDetails";
import { HelpItem } from "../../components/help-enabler/HelpItem";

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
  const { control, watch, setValue } = useFormContext<ClientForm>();
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
                forLabel={t("authentication")}
                forID={t(`common:helpLabel`, { label: t("authentication") })}
              />
            }
          >
            <Controller
              name="publicClient"
              defaultValue={false}
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  data-testid="authentication"
                  id="kc-authentication-switch"
                  name="publicClient"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={!value}
                  onChange={(value) => {
                    onChange(!value);
                    if (!value) {
                      setValue("authorizationServicesEnabled", false);
                      setValue("serviceAccountsEnabled", false);
                    }
                  }}
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
                forLabel={t("authorization")}
                forID={t(`common:helpLabel`, { label: t("authorization") })}
              />
            }
          >
            <Controller
              name="authorizationServicesEnabled"
              defaultValue={false}
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  data-testid="authorization"
                  id="kc-authorization-switch"
                  name="authorizationServicesEnabled"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value && !clientAuthentication}
                  onChange={(value) => {
                    onChange(value);
                    if (value) {
                      setValue("serviceAccountsEnabled", true);
                    }
                  }}
                  isDisabled={clientAuthentication}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            hasNoPaddingTop
            label={t("authenticationFlow")}
            fieldId="kc-flow"
          >
            <Grid>
              <GridItem lg={4} sm={6}>
                <Controller
                  name="standardFlowEnabled"
                  defaultValue={true}
                  control={control}
                  render={({ onChange, value }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="standard"
                        label={t("standardFlow")}
                        id="kc-flow-standard"
                        name="standardFlowEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                      <HelpItem
                        helpText="clients-help:standardFlow"
                        forLabel={t("standardFlow")}
                        forID={t(`common:helpLabel`, {
                          label: t("standardFlow"),
                        })}
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
                  render={({ onChange, value }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="direct"
                        label={t("directAccess")}
                        id="kc-flow-direct"
                        name="directAccessGrantsEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                      <HelpItem
                        helpText="clients-help:directAccess"
                        forLabel={t("directAccess")}
                        forID={t(`common:helpLabel`, {
                          label: t("directAccess"),
                        })}
                      />
                    </InputGroup>
                  )}
                />
              </GridItem>
              <GridItem lg={4} sm={6}>
                <Controller
                  name="implicitFlowEnabled"
                  defaultValue={false}
                  control={control}
                  render={({ onChange, value }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="implicit"
                        label={t("implicitFlow")}
                        id="kc-flow-implicit"
                        name="implicitFlowEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                      <HelpItem
                        helpText="clients-help:implicitFlow"
                        forLabel={t("implicitFlow")}
                        forID={t(`common:helpLabel`, {
                          label: t("implicitFlow"),
                        })}
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
                  render={({ onChange, value }) => (
                    <InputGroup>
                      <Checkbox
                        data-testid="service-account"
                        label={t("serviceAccount")}
                        id="kc-flow-service-account"
                        name="serviceAccountsEnabled"
                        isChecked={
                          value || (clientAuthentication && authorization)
                        }
                        onChange={onChange}
                        isDisabled={
                          (clientAuthentication && !authorization) ||
                          (!clientAuthentication && authorization)
                        }
                      />
                      <HelpItem
                        helpText="clients-help:serviceAccount"
                        forLabel={t("serviceAccount")}
                        forID={t(`common:helpLabel`, {
                          label: t("serviceAccount"),
                        })}
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
                forLabel={t("encryptAssertions")}
                forID={t(`common:helpLabel`, {
                  label: t("encryptAssertions"),
                })}
              />
            }
            label={t("encryptAssertions")}
            fieldId="kc-encrypt"
            hasNoPaddingTop
          >
            <Controller
              name="attributes.saml-encrypt"
              control={control}
              defaultValue="false"
              render={({ onChange, value }) => (
                <Switch
                  data-testid="encrypt"
                  id="kc-encrypt"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange("" + value)}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            labelIcon={
              <HelpItem
                helpText="clients-help:clientSignature"
                forLabel={t("clientSignature")}
                forID={t(`common:helpLabel`, { label: t("clientSignature") })}
              />
            }
            label={t("clientSignature")}
            fieldId="kc-client-signature"
            hasNoPaddingTop
          >
            <Controller
              name="attributes.saml-client-signature"
              control={control}
              defaultValue="false"
              render={({ onChange, value }) => (
                <Switch
                  data-testid="client-signature"
                  id="kc-client-signature"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange("" + value)}
                />
              )}
            />
          </FormGroup>
        </>
      )}
    </FormAccess>
  );
};
