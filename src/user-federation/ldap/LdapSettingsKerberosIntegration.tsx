import { FormGroup, Switch } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useEffect } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import { convertToFormValues } from "../../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useParams } from "react-router-dom";

export const LdapSettingsKerberosIntegration = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const adminClient = useAdminClient();
  const { control, setValue } = useForm<ComponentRepresentation>();
  const { id } = useParams<{ id: string }>();

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", setValue);
      } else {
        setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
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
            name="config.allowKerberosAuthentication"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-allow-kerberos-authentication"}
                isDisabled={false}
                onChange={onChange}
                isChecked={value[0] === "true"}
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
            name="config.useKerberosForPasswordAuthentication"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-use-kerberos-password-authentication"}
                isDisabled={false}
                onChange={onChange}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
