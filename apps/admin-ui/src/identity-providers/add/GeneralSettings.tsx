import { RedirectUrl } from "../component/RedirectUrl";
import { ClientIdSecret } from "../component/ClientIdSecret";
import { DisplayOrder } from "../component/DisplayOrder";

type GeneralSettingsProps = {
  id: string;
  create?: boolean;
};

export const GeneralSettings = ({
  create = true,
  id,
}: GeneralSettingsProps) => (
  <>
    <RedirectUrl id={id} />
    <ClientIdSecret create={create} />
    <DisplayOrder />
  </>
);
