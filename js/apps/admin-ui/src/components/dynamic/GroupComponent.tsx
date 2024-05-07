import {
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  InputGroup,
  InputGroupItem,
} from "@patternfly/react-core";
import { useState } from "react";
import {
  Controller,
  ControllerRenderProps,
  FieldValues,
  useFormContext,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import {
  GroupPickerDialog,
  GroupPickerDialogProps,
} from "../group/GroupPickerDialog";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const GroupComponent = ({
  name,
  label,
  helpText,
  required,
  options,
  defaultValue,
}: ComponentProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const { control } = useFormContext();

  const selectType: GroupPickerDialogProps["type"] = options?.includes("multi")
    ? "selectMany"
    : "selectOne";

  const remove = (
    field: ControllerRenderProps<FieldValues, string>,
    group: string,
  ) =>
    Array.isArray(field.value)
      ? field.onChange(
          field.value.filter((toRemove: string) => group != toRemove),
        )
      : field.onChange("");

  return (
    <Controller
      name={convertToName(name!)}
      defaultValue={defaultValue || ""}
      control={control}
      render={({ field }) => (
        <>
          {open && (
            <GroupPickerDialog
              type={selectType}
              text={{
                title: "selectGroup",
                ok: "select",
              }}
              onConfirm={(groups) => {
                field.onChange(
                  selectType == "selectMany"
                    ? groups?.map((g) => g.path)
                    : groups?.[0].path,
                );
                setOpen(false);
              }}
              onClose={() => setOpen(false)}
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
                  {field.value &&
                    (Array.isArray(field.value) ? (
                      field.value.map((g: string) => (
                        <Chip key={g} onClick={() => remove(field, g)}>
                          {g}
                        </Chip>
                      ))
                    ) : (
                      <Chip onClick={() => remove(field, field.value)}>
                        {field.value}
                      </Chip>
                    ))}
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
