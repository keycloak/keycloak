import { useTranslation } from "react-i18next";
import { Label } from "@patternfly/react-core";

type MoreLabelProps = {
  array: unknown[] | undefined;
};

export const MoreLabel = ({ array }: MoreLabelProps) => {
  const { t } = useTranslation();

  if (!array || array.length <= 1) {
    return null;
  }
  return <Label color="blue">{t("more", { count: array.length - 1 })}</Label>;
};
