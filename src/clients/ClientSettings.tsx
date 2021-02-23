import React from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  TextInput,
  Form,
  Switch,
  TextArea,
  ActionGroup,
  Button,
} from "@patternfly/react-core";
import { Controller, UseFormMethods } from "react-hook-form";

import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { ClientDescription } from "./ClientDescription";
import { CapabilityConfig } from "./add/CapabilityConfig";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";

type ClientSettingsProps = {
  form: UseFormMethods;
  save: () => void;
};

export const ClientSettings = ({ form, save }: ClientSettingsProps) => {
  const { t } = useTranslation("clients");

  return (
    <>
      <ScrollForm
        sections={[
          t("capabilityConfig"),
          t("generalSettings"),
          t("accessSettings"),
          t("loginSettings"),
        ]}
      >
        <CapabilityConfig form={form} />
        <Form isHorizontal>
          <ClientDescription form={form} />
        </Form>
        <FormAccess isHorizontal role="manage-clients">
          <FormGroup label={t("rootUrl")} fieldId="kc-root-url">
            <TextInput
              type="text"
              id="kc-root-url"
              name="rootUrl"
              ref={form.register}
            />
          </FormGroup>
          <FormGroup label={t("validRedirectUri")} fieldId="kc-redirect">
            <MultiLineInput form={form} name="redirectUris" />
          </FormGroup>
          <FormGroup label={t("homeURL")} fieldId="kc-home-url">
            <TextInput
              type="text"
              id="kc-home-url"
              name="baseUrl"
              ref={form.register}
            />
          </FormGroup>
          <FormGroup
            label={t("webOrigins")}
            fieldId="kc-web-origins"
            labelIcon={
              <HelpItem
                helpText="clients-help:webOrigins"
                forLabel={t("webOrigins")}
                forID="kc-web-origins"
              />
            }
          >
            <MultiLineInput form={form} name="webOrigins" />
          </FormGroup>
          <FormGroup
            label={t("adminURL")}
            fieldId="kc-admin-url"
            labelIcon={
              <HelpItem
                helpText="clients-help:adminURL"
                forLabel={t("adminURL")}
                forID="kc-admin-url"
              />
            }
          >
            <TextInput
              type="text"
              id="kc-admin-url"
              name="adminUrl"
              ref={form.register}
            />
          </FormGroup>
        </FormAccess>
        <FormAccess isHorizontal role="manage-clients">
          <FormGroup
            label={t("consentRequired")}
            fieldId="kc-consent"
            hasNoPaddingTop
          >
            <Controller
              name="consentRequired"
              defaultValue={false}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-consent"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("displayOnClient")}
            fieldId="kc-display-on-client"
            hasNoPaddingTop
          >
            <Controller
              name="attributes.display_on_consent_screen"
              defaultValue={false}
              control={form.control}
              render={({ onChange, value }) => (
                <Switch
                  id="kc-display-on-client"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value}
                  onChange={onChange}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("consentScreenText")}
            fieldId="kc-consent-screen-text"
          >
            <TextArea
              id="kc-consent-screen-text"
              name="attributes.consent_screen_text"
              ref={form.register}
            />
          </FormGroup>
          <ActionGroup>
            <Button variant="primary" onClick={() => save()}>
              {t("common:save")}
            </Button>
            <Button variant="link">{t("common:cancel")}</Button>
          </ActionGroup>
        </FormAccess>
      </ScrollForm>
    </>
  );
};
