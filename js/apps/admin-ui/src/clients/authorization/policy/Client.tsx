import { SelectVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { ClientSelect } from "../../../components/client/ClientSelect";

export const Client = () => {
  const { t } = useTranslation();

  return (
    <ClientSelect
      name="clients"
      label={t("clients")}
      helpText={t("policyClientHelp")}
      required
      defaultValue={[]}
      variant={SelectVariant.typeaheadMulti}
    />
  );
};
