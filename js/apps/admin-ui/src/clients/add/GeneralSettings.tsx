import { useTranslation } from "react-i18next";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { ClientDescription } from "../ClientDescription";
import { getProtocolName } from "../utils";

export const GeneralSettings = () => {
  const { t } = useTranslation();
  const providers = useLoginProviders();

  return (
    <FormAccess isHorizontal role="manage-clients">
      <SelectControl
        name="protocol"
        label={t("clientType")}
        labelIcon={t("clientTypeHelp")}
        controller={{
          defaultValue: "",
        }}
        options={providers.map((option) => ({
          key: option,
          value: getProtocolName(t, option),
        }))}
      />
      <ClientDescription hasConfigureAccess />
    </FormAccess>
  );
};
