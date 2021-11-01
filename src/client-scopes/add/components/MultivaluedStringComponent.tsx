import React from "react";
import { useTranslation } from "react-i18next";
import { FormGroup } from "@patternfly/react-core";
import { camelCase } from "lodash";

import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { MultiLineInput } from "../../../components/multi-line-input/MultiLineInput";
import { convertToHyphens } from "../../../util";

export const MultiValuedStringComponent = ({
  name,
  label,
  helpText,
}: ComponentProps) => {
  const { t } = useTranslation("realm-settings");

  const convertToString = (s: string) =>
    camelCase(convertToHyphens(s).replaceAll("-", " "));

  return (
    <FormGroup
      label={t(convertToString(label!).replace("Label", ""))}
      labelIcon={
        <HelpItem
          helpText={t(`realm-settings-help:${convertToString(helpText!)}`)}
          forLabel={t(label!)}
          forID={name!}
        />
      }
      fieldId={name!}
    >
      <MultiLineInput
        name={`config.${convertToHyphens(name!)}`}
        aria-label={name}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: t(
            camelCase(
              convertToHyphens(label!).replaceAll("-", " ").replace("label", "")
            )
          ).toLowerCase(),
        })}
      />
    </FormGroup>
  );
};
