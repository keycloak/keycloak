import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, TextInput } from "@patternfly/react-core";

import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";

export const StringComponent = ({
  name,
  label,
  helpText,
  defaultValue,
}: ComponentProps) => {
  const { t } = useTranslation("client-scopes");
  const { register } = useFormContext();

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} forLabel={t(label!)} forID={name!} />
      }
      fieldId={name!}
    >
      <TextInput
        id={name!}
        data-testid={name}
        ref={register()}
        type="text"
        name={`config.${name?.replaceAll(".", "-")}`}
        defaultValue={defaultValue?.toString()}
      />
    </FormGroup>
  );
};
