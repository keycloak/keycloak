import { useTranslation } from "react-i18next";
import { SelectControl } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { useLoginProviders } from "../../context/server-info/ServerInfoProvider";
import { ClientDescription } from "../ClientDescription";
import { getProtocolName } from "../utils";
import { PROTOCOL_OID4VC } from "../constants";

export const GeneralSettings = () => {
  const { t } = useTranslation();
  const providers = useLoginProviders();

  const filteredProviders = providers.filter(
    (provider) => provider !== PROTOCOL_OID4VC,
  );

  return (
    <FormAccess isHorizontal role="manage-clients">
      <SelectControl
        name="protocol"
        label={t("clientType")}
        labelIcon={t("clientTypeHelp")}
        controller={{
          defaultValue: "",
        }}
        options={filteredProviders.map((option) => ({
          key: option,
          value: getProtocolName(t, option),
        }))}
      />
      <ClientDescription hasConfigureAccess />
    </FormAccess>
  );
};
