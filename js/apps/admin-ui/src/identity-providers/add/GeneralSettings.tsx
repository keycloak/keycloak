import { ClientIdSecret } from "../component/ClientIdSecret";
import { DisplayOrder } from "../component/DisplayOrder";
import { OIDCGeneralSettings } from "./OIDCGeneralSettings";

type GeneralSettingsProps = {
  create?: boolean;
};

export const GeneralSettings = ({ create = true }: GeneralSettingsProps) => (
  <>
    <OIDCGeneralSettings />
    <ClientIdSecret create={create} />
    <DisplayOrder />
  </>
);
