import { SwitchProps } from "@patternfly/react-core";
import { FieldPath, FieldValues, UseControllerProps } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SwitchControl } from "ui-shared";

type AdminSwitchControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
> = SwitchProps &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    labelIcon?: string;
  };

export const DefaultSwitchControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>
>(
  props: AdminSwitchControlProps<T, P>
) => {
  const { t } = useTranslation("common");

  return <SwitchControl {...props} labelOn={t("on")} labelOff={t("off")} />;
};
