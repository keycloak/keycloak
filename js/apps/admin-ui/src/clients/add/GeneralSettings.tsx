import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { ClientDescription } from "../ClientDescription";
import { getProtocolName } from "../utils";

export const GeneralSettings = () => {
  const { t } = useTranslation();
  const {
    control,
    formState: { errors },
  } = useFormContext();

  const providers = useLoginProviders();
  const [open, isOpen] = useState(false);

  return (
    <FormAccess isHorizontal role="manage-clients">
      <FormGroup
        label={t("clientType")}
        fieldId="kc-type"
        validated={errors.protocol ? "error" : "default"}
        labelIcon={
          <HelpItem helpText={t("clientTypeHelp")} fieldLabelId="clientType" />
        }
      >
        <Controller
          name="protocol"
          defaultValue=""
          control={control}
          render={({ field }) => (
            <Select
              id="kc-type"
              onToggle={isOpen}
              onSelect={(_, value) => {
                field.onChange(value.toString());
                isOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              aria-label={t("selectEncryptionType")}
              isOpen={open}
            >
              {providers.map((option) => (
                <SelectOption
                  selected={option === field.value}
                  key={option}
                  value={option}
                  data-testid={`option-${option}`}
                >
                  {getProtocolName(t, option)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <ClientDescription hasConfigureAccess />
    </FormAccess>
  );
};
