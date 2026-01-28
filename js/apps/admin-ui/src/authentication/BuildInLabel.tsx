import { Label } from "@patternfly/react-core";
import { CheckCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

import style from "./build-in-label.module.css";

export const BuildInLabel = () => {
  const { t } = useTranslation();

  return (
    <Label icon={<CheckCircleIcon className={style.icon} />}>
      {t("buildIn")}
    </Label>
  );
};
