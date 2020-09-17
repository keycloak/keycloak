import React from "react";
import {
  FormGroup,
  Switch,
  Checkbox,
  Grid,
  GridItem,
  Form,
} from "@patternfly/react-core";
import { UseFormMethods, Controller } from "react-hook-form";
import { useTranslation } from "react-i18next";

type Step2Props = {
  form: UseFormMethods;
};

export const Step2 = ({ form }: Step2Props) => {
  const { t } = useTranslation("clients");
  return (
    <Form isHorizontal>
      <FormGroup label={t("clientAuthentication")} fieldId="kc-authentication">
        <Controller
          name="publicClient"
          control={form.control}
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
      <FormGroup label={t("authentication")} fieldId="kc-authorisation">
        <Controller
          name="authorizationServicesEnabled"
          control={form.control}
          render={({ onChange, value }) => (
            <Switch
              id="kc-authorisation"
              name="authorizationServicesEnabled"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value}
              onChange={onChange}
            />
          )}
        />
      </FormGroup>
      <FormGroup label={t("authenticationFlow")} fieldId="kc-flow">
        <Grid>
          <GridItem span={6}>
            <Controller
              name="standardFlowEnabled"
              control={form.control}
              render={({ onChange, value }) => (
                <Checkbox
                  label={t("standardFlow")}
                  aria-label={t("enableStandardFlow")}
                  id="kc-flow-standard"
                  name="standardFlowEnabled"
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </GridItem>
          <GridItem span={6}>
            <Controller
              name="directAccessGrantsEnabled"
              control={form.control}
              render={({ onChange, value }) => (
                <Checkbox
                  label={t("directAccess")}
                  aria-label={t("enableDirectAccess")}
                  id="kc-flow-direct"
                  name="directAccessGrantsEnabled"
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </GridItem>
          <GridItem span={6}>
            <Controller
              name="implicitFlowEnabled"
              control={form.control}
              render={({ onChange, value }) => (
                <Checkbox
                  label={t("implicidFlow")}
                  aria-label={t("enableImplicidFlow")}
                  id="kc-flow-implicid"
                  name="implicitFlowEnabled"
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </GridItem>
          <GridItem span={6}>
            <Controller
              name="serviceAccountsEnabled"
              control={form.control}
              render={({ onChange, value }) => (
                <Checkbox
                  label={t("serviceAccount")}
                  aria-label={t("enableServiceAccount")}
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
    </Form>
  );
};
