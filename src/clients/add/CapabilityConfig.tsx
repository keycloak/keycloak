import React from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  Switch,
  Checkbox,
  Grid,
  GridItem,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { ClientForm } from "../ClientDetails";
import { HelpItem } from "../../components/help-enabler/HelpItem";

type CapabilityConfigProps = {
  unWrap?: boolean;
  protocol?: string;
};

export const CapabilityConfig = ({
  unWrap,
  protocol: type,
}: CapabilityConfigProps) => {
  const { t } = useTranslation("clients");
  const { control, watch } = useFormContext<ClientForm>();
  const protocol = type || watch("protocol");

  return (
    <FormAccess isHorizontal role="manage-clients" unWrap={unWrap}>
      <>
        {protocol === "openid-connect" && (
          <>
            <FormGroup
              hasNoPaddingTop
              label={t("clientAuthentication")}
              fieldId="kc-authentication"
            >
              <Controller
                name="publicClient"
                defaultValue={false}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-authentication"
                    name="publicClient"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
                  />
                )}
              />
            </FormGroup>
            <FormGroup
              hasNoPaddingTop
              label={t("clientAuthorization")}
              fieldId="kc-authorization"
            >
              <Controller
                name="authorizationServicesEnabled"
                defaultValue={false}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    id="kc-authorization"
                    name="authorizationServicesEnabled"
                    label={t("common:on")}
                    labelOff={t("common:off")}
                    isChecked={value}
                    onChange={onChange}
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
                    defaultValue={false}
                    control={control}
                    render={({ onChange, value }) => (
                      <Checkbox
                        label={t("standardFlow")}
                        id="kc-flow-standard"
                        name="standardFlowEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                    )}
                  />
                </GridItem>
                <GridItem lg={8} sm={6}>
                  <Controller
                    name="directAccessGrantsEnabled"
                    defaultValue={false}
                    control={control}
                    render={({ onChange, value }) => (
                      <Checkbox
                        label={t("directAccess")}
                        id="kc-flow-direct"
                        name="directAccessGrantsEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                    )}
                  />
                </GridItem>
                <GridItem lg={4} sm={6}>
                  <Controller
                    name="implicitFlowEnabled"
                    defaultValue={false}
                    control={control}
                    render={({ onChange, value }) => (
                      <Checkbox
                        label={t("implicitFlow")}
                        id="kc-flow-implicit"
                        name="implicitFlowEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                    )}
                  />
                </GridItem>
                <GridItem lg={8} sm={6}>
                  <Controller
                    name="serviceAccountsEnabled"
                    defaultValue={false}
                    control={control}
                    render={({ onChange, value }) => (
                      <Checkbox
                        label={t("serviceAccount")}
                        id="kc-flow-service-account"
                        name="serviceAccountsEnabled"
                        isChecked={value}
                        onChange={onChange}
                      />
                    )}
                  />
                </GridItem>
              </Grid>
            </FormGroup>
          </>
        )}
      </>
      <>
        {protocol === "saml" && (
          <>
            <FormGroup
              labelIcon={
                <HelpItem
                  helpText="clients-help:encryptAssertions"
                  forLabel={t("encryptAssertions")}
                  forID="kc-encrypt"
                />
              }
              label={t("encryptAssertions")}
              fieldId="kc-encrypt"
            >
              <Controller
                name="attributes.saml_encrypt"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Switch
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
                  forID="kc-client-signature"
                />
              }
              label={t("clientSignature")}
              fieldId="kc-client-signature"
            >
              <Controller
                name="attributes.saml_client_signature"
                control={control}
                defaultValue="false"
                render={({ onChange, value }) => (
                  <Switch
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
      </>
    </FormAccess>
  );
};
