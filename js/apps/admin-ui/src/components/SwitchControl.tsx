import { SwitchProps } from "@patternfly/react-core";
import { FieldPath, FieldValues, UseControllerProps } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { SwitchControl } from "ui-shared";

type DefaultSwitchControlProps<
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
> = SwitchProps &
  UseControllerProps<T, P> & {
    name: string;
    label?: string;
    labelIcon?: string;
    stringify?: boolean;
  };

export const DefaultSwitchControl = <
  T extends FieldValues,
  P extends FieldPath<T> = FieldPath<T>,
>(
  props: DefaultSwitchControlProps<T, P>,
) => {
  const { t } = useTranslation();

  return <SwitchControl {...props} labelOn={t("on")} labelOff={t("off")} />;
};
