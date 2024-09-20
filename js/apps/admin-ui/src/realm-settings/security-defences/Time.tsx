import { CSSProperties } from "react";
import { useTranslation } from "react-i18next";

import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";

export const Time = ({
  name,
  style,
}: {
  name: string;
  style?: CSSProperties;
}) => {
  const { t } = useTranslation();
  return (
    <TimeSelectorControl
      name={name}
      style={style}
      label={t(name)}
      labelIcon={t(`${name}Help`)}
      controller={{
        defaultValue: "",
        rules: { required: t("required") },
      }}
    />
  );
};
