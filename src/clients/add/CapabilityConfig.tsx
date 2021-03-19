import React from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  Switch,
  Checkbox,
  Grid,
  GridItem,
  InputGroup,
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
  const { control, watch, setValue } = useFormContext<ClientForm>();
  const protocol = type || watch("protocol");
  const clientAuthentication = watch("publicClient");
  const clientAuthorization = watch("authorizationServicesEnabled");

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
                    onChange={(value) => {
                      onChange(value);
                      if (value) {
                        setValue("serviceAccountsEnabled", true);
                      }
                    }}
                    isDisabled={!clientAuthentication}
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
                <GridItem lg={3} sm={6}>
                  <Controller
                    name="standardFlowEnabled"
                    defaultValue={true}
                    control={control}
                    render={({ onChange, value }) => (
                      <InputGroup>
                        <Checkbox
                          label={t("standardFlow")}
                          id="kc-flow-standard"
                          name="standardFlowEnabled"
                          isChecked={value}
                          onChange={onChange}
                        />
                        <HelpItem
                          helpText="clients-help:standardFlow"
                          forLabel={t("standardFlow")}
                          forID="kc-flow-standard"
                        />
                      </InputGroup>
                    )}
                  />
                </GridItem>
                <GridItem lg={9} sm={6}>
                  <Controller
                    name="directAccessGrantsEnabled"
                    defaultValue={true}
                    control={control}
                    render={({ onChange, value }) => (
                      <InputGroup>
                        <Checkbox
                          label={t("directAccess")}
                          id="kc-flow-direct"
                          name="directAccessGrantsEnabled"
                          isChecked={value}
                          onChange={onChange}
                        />
                        <HelpItem
                          helpText="clients-help:directAccess"
                          forLabel={t("directAccess")}
                          forID="kc-flow-direct"
                        />
                      </InputGroup>
                    )}
                  />
                </GridItem>
                <GridItem lg={3} sm={6}>
                  <Controller
                    name="implicitFlowEnabled"
                    defaultValue={false}
                    control={control}
                    render={({ onChange, value }) => (
                      <InputGroup>
                        <Checkbox
                          label={t("implicitFlow")}
                          id="kc-flow-implicit"
                          name="implicitFlowEnabled"
                          isChecked={value}
                          onChange={onChange}
                        />
                        <HelpItem
                          helpText="clients-help:implicitFlow"
                          forLabel={t("implicitFlow")}
                          forID="kc-flow-implicit"
                        />
                      </InputGroup>
                    )}
                  />
                </GridItem>
                <GridItem lg={9} sm={6}>
                  <Controller
                    name="serviceAccountsEnabled"
                    defaultValue={false}
                    control={control}
                    render={({ onChange, value }) => (
                      <InputGroup>
                        <Checkbox
                          label={t("serviceAccount")}
                          id="kc-flow-service-account"
                          name="serviceAccountsEnabled"
                          isChecked={value}
                          onChange={onChange}
                          isDisabled={
                            !clientAuthentication || clientAuthorization
                          }
                        />
                        <HelpItem
                          helpText="clients-help:serviceAccount"
                          forLabel={t("serviceAccount")}
                          forID="kc-flow-service-account"
                        />
                      </InputGroup>
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
