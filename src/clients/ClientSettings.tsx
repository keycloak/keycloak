import React, { useState, FormEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  TextInput,
  Form,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  Switch,
  TextArea,
  PageSection,
} from "@patternfly/react-core";
import { useForm } from "react-hook-form";

import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { ClientDescription } from "./ClientDescription";
import { ClientRepresentation } from "./models/client-model";
import { CapabilityConfig } from "./add/CapabilityConfig";

type ClientSettingsProps = {
  client: ClientRepresentation;
};

export const ClientSettings = ({ client: clientInit }: ClientSettingsProps) => {
  const { t } = useTranslation("clients");
  const [client, setClient] = useState({ ...clientInit });
  const form = useForm();
  const onChange = (
    value: string | boolean,
    event: FormEvent<HTMLInputElement>
  ) => {
    const target = event.target;
    const name = (target as HTMLInputElement).name;

    setClient({
      ...client,
      [name]: value,
    });
  };
  return (
    <PageSection>
      <ScrollForm
        sections={[
          t("capabilityConfig"),
          t("generalSettings"),
          t("accessSettings"),
          t("loginSettings"),
        ]}
      >
        <Form isHorizontal>
          <CapabilityConfig form={form} />
        </Form>
        <Form isHorizontal>
          <ClientDescription register={form.register} />
        </Form>
        <Form isHorizontal>
          <FormGroup label={t("rootUrl")} fieldId="kc-root-url">
            <TextInput
              type="text"
              id="kc-root-url"
              name="rootUrl"
              value={client.rootUrl}
              onChange={onChange}
            />
          </FormGroup>
          <FormGroup label={t("validRedirectUri")} fieldId="kc-redirect">
            <TextInput
              type="text"
              id="kc-redirect"
              name="redirectUris"
              onChange={onChange}
            />
          </FormGroup>
          <FormGroup label={t("homeURL")} fieldId="kc-home-url">
            <TextInput
              type="text"
              id="kc-home-url"
              name="baseUrl"
              value={client.baseUrl}
              onChange={onChange}
            />
          </FormGroup>
        </Form>
        <Form isHorizontal>
          <FormGroup label={t("loginTheme")} fieldId="kc-login-theme">
            <Dropdown
              id="kc-login-theme"
              toggle={
                <DropdownToggle id="toggle-id" onToggle={() => {}}>
                  {t("loginTheme")}
                </DropdownToggle>
              }
              dropdownItems={[
                <DropdownItem key="link">Link</DropdownItem>,
                <DropdownItem key="action" component="button" />,
              ]}
            />
          </FormGroup>
          <FormGroup label={t("consentRequired")} fieldId="kc-consent">
            <Switch
              id="kc-consent"
              name="consentRequired"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={client.consentRequired}
              onChange={onChange}
            />
          </FormGroup>
          <FormGroup
            label={t("displayOnClient")}
            fieldId="kc-display-on-client"
          >
            <Switch
              id="kc-display-on-client"
              name="alwaysDisplayInConsole"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={client.alwaysDisplayInConsole}
              onChange={onChange}
            />
          </FormGroup>
          <FormGroup
            label={t("consentScreenText")}
            fieldId="kc-consent-screen-text"
          >
            <TextArea
              id="kc-consent-screen-text"
              name="consentText"
              //value={client.protocolMappers![0].consentText}
            />
          </FormGroup>
        </Form>
      </ScrollForm>
    </PageSection>
  );
};
