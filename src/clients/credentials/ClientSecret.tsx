import React from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  ClipboardCopy,
  FormGroup,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { ClientForm } from "../ClientDetails";

export type ClientSecretProps = {
  secret: string;
  toggle: () => void;
};

export const ClientSecret = ({ secret, toggle }: ClientSecretProps) => {
  const { t } = useTranslation("clients");
  const { formState } = useFormContext<ClientForm>();
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
            onClick={toggle}
            isDisabled={formState.isDirty}
          >
            {t("regenerate")}
          </Button>
        </SplitItem>
      </Split>
    </FormGroup>
  );
};
