import React from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ClipboardCopy,
  FormGroup,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { UseFormMethods } from "react-hook-form";

export type ClientSecretProps = {
  secret: string;
  toggle: () => void;
  form: UseFormMethods<ClientRepresentation>;
};

export const ClientSecret = ({ secret, toggle, form }: ClientSecretProps) => {
  const { t } = useTranslation("clients");

  return (
    <FormGroup label={t("clientSecret")} fieldId="kc-client-secret">
      <Split hasGutter>
        <SplitItem isFilled>
          <ClipboardCopy id="kc-client-secret" isReadOnly>
            {secret}
          </ClipboardCopy>
        </SplitItem>
        <SplitItem>
          <Button
            variant="secondary"
            isDisabled={form.formState.isDirty}
            onClick={toggle}
          >
            {t("regenerate")}
          </Button>
        </SplitItem>
      </Split>
    </FormGroup>
  );
};
