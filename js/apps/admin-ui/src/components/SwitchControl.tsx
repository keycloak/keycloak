import { FieldPath, FieldValues } from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { SwitchControlProps } from "ui-shared";
import { SwitchControl } from "ui-shared";

export type DefaultSwitchControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = Omit<SwitchControlProps<T, P>, "labelOn" | "labelOff">;

export const DefaultSwitchControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>(
  props: DefaultSwitchControlProps<T, P>,
) => {
  const { t } = useTranslation();

  return <SwitchControl {...props} labelOn={t("on")} labelOff={t("off")} />;
};
