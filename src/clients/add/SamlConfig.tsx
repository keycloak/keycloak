import React, { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";

import type { ClientForm } from "../ClientDetails";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";

export const Toggle = ({ name, label }: { name: string; label: string }) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext<ClientForm>();

  return (
    <FormGroup
      hasNoPaddingTop
      label={t(label)}
      fieldId={label}
      labelIcon={
        <HelpItem
          helpText={t(`clients-help:${label}`)}
          fieldLabelId={`clients:${label}`}
        />
      }
    >
      <Controller
        name={name}
        defaultValue="false"
        control={control}
        render={({ onChange, value }) => (
          <Switch
            id={name!}
            data-testid={label}
            label={t("common:on")}
            labelOff={t("common:off")}
            isChecked={value === "true"}
            onChange={(value) => onChange(value.toString())}
          />
        )}
      />
    </FormGroup>
  );
};

export const SamlConfig = () => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext<ClientForm>();

  const [nameFormatOpen, setNameFormatOpen] = useState(false);
  return (
    <FormAccess
      isHorizontal
      role="manage-clients"
      className="keycloak__capability-config__form"
    >
      <FormGroup
        label={t("nameIdFormat")}
        fieldId="nameIdFormat"
        labelIcon={
          <HelpItem
            helpText="clients-help:nameIdFormat"
            fieldLabelId="clients:nameIdFormat"
          />
        }
      >
        <Controller
          name="attributes.saml_name_id_format"
          defaultValue="username"
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="samlNameIdFormat"
              onToggle={setNameFormatOpen}
              onSelect={(_, value) => {
                onChange(value.toString());
                setNameFormatOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
              aria-label={t("nameIdFormat")}
              isOpen={nameFormatOpen}
            >
              {["username", "email", "transient", "persistent"].map((name) => (
                <SelectOption
                  selected={name === value}
                  key={name}
                  value={name}
                />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <Toggle
        name="attributes.saml.force.name.id.format"
        label="forceNameIdFormat"
      />
      <Toggle
        name="attributes.saml.force.post.binding"
        label="forcePostBinding"
      />
      <Toggle
        name="attributes.saml.artifact.binding"
        label="forceArtifactBinding"
      />
      <Toggle
        name="attributes.saml.onetimeuse.condition"
        label="includeOneTimeUseCondition"
      />
      <Toggle
        name="attributes.saml.server.signature.keyinfo.ext"
        label="optimizeLookup"
      />
    </FormAccess>
  );
};
