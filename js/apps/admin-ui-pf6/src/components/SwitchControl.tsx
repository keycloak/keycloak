import { FieldPath, FieldValues } from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { SwitchControlProps } from "@keycloak/keycloak-ui-shared-pf6";
import { SwitchControl } from "@keycloak/keycloak-ui-shared-pf6";

export type DefaultSwitchControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<SwitchControlProps<T, P>, "labelOn">;

export const DefaultSwitchControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>(
  props: DefaultSwitchControlProps<T, P>,
) => {
  const { t } = useTranslation();

  return <SwitchControl {...props} labelOn={t("on")} />;
};
