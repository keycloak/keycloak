import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { sortProviders } from "../../util";
import { ClientIdSecret } from "../component/ClientIdSecret";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

const clientAuthentications = [
  "client_secret_post",
  "client_secret_basic",
  "client_secret_jwt",
  "private_key_jwt",
];

export const OIDCAuthentication = ({ create = true }: { create?: boolean }) => {
  const providers = useServerInfo().providers!.clientSignature.providers;
  const { t } = useTranslation();

  const { control } = useFormContext();
  const [openClientAuth, setOpenClientAuth] = useState(false);
  const [openClientAuthSigAlg, setOpenClientAuthSigAlg] = useState(false);

  const clientAuthMethod = useWatch({
    control: control,
    name: "config.clientAuthMethod",
  });

  return (
    <>
      <FormGroup
        label={t("clientAuthentication")}
        labelIcon={
          <HelpItem
            helpText={t("clientAuthenticationHelp")}
            fieldLabelId="clientAuthentication"
          />
        }
        fieldId="clientAuthentication"
      >
        <Controller
          name="config.clientAuthMethod"
          defaultValue={clientAuthentications[0]}
          control={control}
          render={({ field }) => (
            <KeycloakSelect
              toggleId="clientAuthentication"
              onToggle={() => setOpenClientAuth(!openClientAuth)}
              onSelect={(value) => {
                field.onChange(value as string);
                setOpenClientAuth(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              aria-label={t("clientAuthentication")}
              isOpen={openClientAuth}
            >
              {clientAuthentications.map((option) => (
                <SelectOption
                  selected={option === field.value}
                  key={option}
                  value={option}
                >
                  {t(`clientAuthentications.${option}`)}
                </SelectOption>
              ))}
            </KeycloakSelect>
          )}
        />
      </FormGroup>
      <ClientIdSecret
        secretRequired={clientAuthMethod !== "private_key_jwt"}
        create={create}
      />
      <FormGroup
        label={t("clientAssertionSigningAlg")}
        labelIcon={
          <HelpItem
            helpText={t("clientAssertionSigningAlgHelp")}
            fieldLabelId="clientAssertionSigningAlg"
          />
        }
        fieldId="clientAssertionSigningAlg"
      >
        <Controller
          name="config.clientAssertionSigningAlg"
          defaultValue=""
          control={control}
          render={({ field }) => (
            <KeycloakSelect
              maxHeight={200}
              toggleId="clientAssertionSigningAlg"
              onToggle={() => setOpenClientAuthSigAlg(!openClientAuthSigAlg)}
              onSelect={(value) => {
                field.onChange(value.toString());
                setOpenClientAuthSigAlg(false);
              }}
              selections={field.value || t("algorithmNotSpecified")}
              variant={SelectVariant.single}
              aria-label={t("selectClientAssertionSigningAlg")}
              isOpen={openClientAuthSigAlg}
            >
              {[
                <SelectOption selected={field.value === ""} key="" value="">
                  {t("algorithmNotSpecified")}
                </SelectOption>,
                ...sortProviders(providers).map((option) => (
                  <SelectOption
                    selected={option === field.value}
                    key={option}
                    value={option}
                  >
                    {option}
                  </SelectOption>
                )),
              ]}
            </KeycloakSelect>
          )}
        />
      </FormGroup>
      {(clientAuthMethod === "private_key_jwt" ||
        clientAuthMethod === "client_secret_jwt") && (
        <TextField
          field="config.clientAssertionAudience"
          label="clientAssertionAudience"
        />
      )}
      {clientAuthMethod === "private_key_jwt" && (
        <SwitchField
          field="config.jwtX509HeadersEnabled"
          label="jwtX509HeadersEnabled"
        />
      )}
    </>
  );
};
