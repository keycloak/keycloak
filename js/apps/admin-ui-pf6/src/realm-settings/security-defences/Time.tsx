import { CSSProperties } from "react";
import { useTranslation } from "react-i18next";

import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";

export const Time = ({
  name,
  style,
  min,
}: {
  name: string;
  style?: CSSProperties;
  min?: number;
}) => {
  const { t } = useTranslation();
  return (
    <TimeSelectorControl
      name={name}
      style={style}
      label={t(name)}
      labelIcon={t(`${name}Help`)}
      min={min}
      controller={{
        defaultValue: "",
        rules: { required: t("required"), min: min },
      }}
    />
  );
};
