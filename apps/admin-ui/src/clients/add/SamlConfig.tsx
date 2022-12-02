import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import {
  Controller,
  Path,
  PathValue,
  useFormContext,
} from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";

type ToggleProps = {
  name: PathValue<FormFields, Path<FormFields>>;
  label: string;
};
export const Toggle = ({ name, label }: ToggleProps) => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext<FormFields>();

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
        render={({ field }) => (
          <Switch
            id={name!}
            data-testid={label}
            label={t("common:on")}
            labelOff={t("common:off")}
            isChecked={field.value === "true"}
            onChange={(value) => field.onChange(value.toString())}
            aria-label={t(label)}
          />
        )}
      />
    </FormGroup>
  );
};

export const SamlConfig = () => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext<FormFields>();

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
          render={({ field }) => (
            <Select
              toggleId="samlNameIdFormat"
              onToggle={setNameFormatOpen}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                setNameFormatOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              aria-label={t("nameIdFormat")}
              isOpen={nameFormatOpen}
            >
              {["username", "email", "transient", "persistent"].map((name) => (
                <SelectOption
                  selected={name === field.value}
                  key={name}
                  value={name}
                />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <Toggle
        name={convertAttributeNameToForm(
          "attributes.saml.force.name.id.format"
        )}
        label="forceNameIdFormat"
      />
      <Toggle
        name={convertAttributeNameToForm("attributes.saml.force.post.binding")}
        label="forcePostBinding"
      />
      <Toggle
        name={convertAttributeNameToForm("attributes.saml.artifact.binding")}
        label="forceArtifactBinding"
      />
      <Toggle
        name={convertAttributeNameToForm("attributes.saml.authnstatement")}
        label="includeAuthnStatement"
      />
      <Toggle
        name={convertAttributeNameToForm(
          "attributes.saml.onetimeuse.condition"
        )}
        label="includeOneTimeUseCondition"
      />
      <Toggle
        name={convertAttributeNameToForm(
          "attributes.saml.server.signature.keyinfo.ext"
        )}
        label="optimizeLookup"
      />
    </FormAccess>
  );
};
