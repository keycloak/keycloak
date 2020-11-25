import { Form, FormGroup, Switch } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

export const LdapSettingsKerberosIntegration = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const { handleSubmit, control } = useForm<ComponentRepresentation>();
  const onSubmit = (data: ComponentRepresentation) => {
    console.log(data);
  };

  return (
    <>
      {/* Kerberos integration */}
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <FormGroup
          label={t("allowKerberosAuthentication")}
          labelIcon={
            <HelpItem
              helpText={helpText("allowKerberosAuthenticationHelp")}
              forLabel={t("allowKerberosAuthentication")}
              forID="kc-allow-kerberos-authentication"
            />
          }
          fieldId="kc-allow-kerberos-authentication"
          hasNoPaddingTop
        >
          <Controller
            name="allowKerberosAuthentication"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-allow-kerberos-authentication"}
                isDisabled={false}
                onChange={onChange}
                isChecked={value}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>

        <FormGroup
          label={t("useKerberosForPasswordAuthentication")}
          labelIcon={
            <HelpItem
              helpText={helpText("useKerberosForPasswordAuthenticationHelp")}
              forLabel={t("useKerberosForPasswordAuthentication")}
              forID="kc-use-kerberos-password-authentication"
            />
          }
          fieldId="kc-use-kerberos-password-authentication"
          hasNoPaddingTop
        >
          <Controller
            name="useKerberosForPasswordAuthentication"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-use-kerberos-password-authentication"}
                isDisabled={false}
                onChange={onChange}
                isChecked={value}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>

        <button type="submit">Test submit</button>
      </Form>
    </>
  );
};
