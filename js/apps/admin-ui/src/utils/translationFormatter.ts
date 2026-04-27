import { label } from "@keycloak/keycloak-ui-shared";
import { IFormatter, IFormatterValueType } from "@patternfly/react-table";
import { TFunction } from "i18next";

export const translationFormatter =
  (t: TFunction): IFormatter =>
  (data?: IFormatterValueType) => {
    return data ? label(t, data as string) || "—" : "—";
  };
