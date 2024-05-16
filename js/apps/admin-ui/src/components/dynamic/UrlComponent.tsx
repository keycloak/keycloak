import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useFormContext, useWatch } from "react-hook-form";
import type { ComponentProps } from "./components";
import { FormattedLink } from "../external-link/FormattedLink";

import "./url-component.css";

export const UrlComponent = ({ name, label, helpText }: ComponentProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const { value } = useWatch({
    control,
    name: name!,
    defaultValue: "",
  });

  return (
    <FormGroup
      label={t(label!)}
      fieldId={name!}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      className="keycloak__identity-providers__url_component"
    >
      <FormattedLink title={t(helpText!)} href={value} isInline />
    </FormGroup>
  );
};
