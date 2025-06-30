import { useTranslation } from "react-i18next";
import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";
import { credResetFormDefaultValues } from "./ResetCredentialDialog";

export const LifespanField = () => {
  const { t } = useTranslation();

  return (
    <TimeSelectorControl
      name="lifespan"
      label={t("lifespan")}
      labelIcon={t("lifespanHelp")}
      units={["minute", "hour", "day"]}
      menuAppendTo="parent"
      controller={{
        defaultValue: credResetFormDefaultValues.lifespan,
      }}
    />
  );
};
