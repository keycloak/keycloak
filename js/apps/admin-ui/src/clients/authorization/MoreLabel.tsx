import { useTranslation } from "react-i18next";
import { Label } from "@patternfly/react-core";

type MoreLabelProps = {
  array: unknown[] | undefined;
};

export const MoreLabel = ({ array }: MoreLabelProps) => {
  const { t } = useTranslation("clients");

  if (!array || array.length <= 1) {
    return null;
  }
  return (
    <Label color="blue">{t("common:more", { count: array.length - 1 })}</Label>
  );
};
