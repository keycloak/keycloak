import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  InputGroup,
} from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "../help-enabler/HelpItem";
import { GroupPickerDialog } from "../group/GroupPickerDialog";

export const GroupComponent = ({ name, label, helpText }: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const [open, setOpen] = useState(false);
  const { control } = useFormContext();

  return (
    <Controller
      name={`config.${name}`}
      defaultValue=""
      typeAheadAriaLabel={t("selectGroup")}
      control={control}
      render={({ onChange, value }) => (
        <>
          {open && (
            <GroupPickerDialog
              type="selectOne"
              text={{
                title: "dynamic:selectGroup",
                ok: "common:select",
              }}
              onConfirm={(groups) => {
                onChange(groups[0].path);
                setOpen(false);
              }}
              onClose={() => setOpen(false)}
              filterGroups={value}
            />
          )}

          <FormGroup
            label={t(label!)}
            labelIcon={
              <HelpItem
                helpText={t(helpText!)}
                fieldLabelId={`dynamic:${label}`}
              />
            }
            fieldId={name!}
          >
            <InputGroup>
              <ChipGroup>
                {value && (
                  <Chip onClick={() => onChange(undefined)}>{value}</Chip>
                )}
              </ChipGroup>
              <Button
                id="kc-join-groups-button"
                onClick={() => setOpen(!open)}
                variant="secondary"
                data-testid="join-groups-button"
              >
                {t("selectGroup")}
              </Button>
            </InputGroup>
          </FormGroup>
        </>
      )}
    />
  );
};
