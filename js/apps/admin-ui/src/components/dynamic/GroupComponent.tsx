import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  InputGroup,
  InputGroupItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { GroupPickerDialog } from "../group/GroupPickerDialog";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import type { ComponentProps } from "./components";

export const GroupComponent = ({
  name,
  label,
  helpText,
  required,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [groups, setGroups] = useState<GroupRepresentation[]>();
  const { control } = useFormContext();

  return (
    <Controller
      name={convertToName(name!)}
      defaultValue=""
      control={control}
      render={({ field }) => (
        <>
          {open && (
            <GroupPickerDialog
              type="selectOne"
              text={{
                title: "selectGroup",
                ok: "select",
              }}
              onConfirm={(groups) => {
                field.onChange(groups?.[0].path);
                setGroups(groups);
                setOpen(false);
              }}
              onClose={() => setOpen(false)}
              filterGroups={groups}
            />
          )}

          <FormGroup
            label={t(label!)}
            labelIcon={
              <HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />
            }
            fieldId={name!}
            isRequired={required}
          >
            <InputGroup>
              <InputGroupItem>
                <ChipGroup>
                  {field.value && (
                    <Chip onClick={() => field.onChange(undefined)}>
                      {field.value}
                    </Chip>
                  )}
                </ChipGroup>
              </InputGroupItem>
              <InputGroupItem>
                <Button
                  id="kc-join-groups-button"
                  onClick={() => setOpen(!open)}
                  variant="secondary"
                  data-testid="join-groups-button"
                >
                  {t("selectGroup")}
                </Button>
              </InputGroupItem>
            </InputGroup>
          </FormGroup>
        </>
      )}
    />
  );
};
