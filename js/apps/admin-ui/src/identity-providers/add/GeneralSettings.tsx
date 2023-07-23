import { RedirectUrl } from "../component/RedirectUrl";
import { ClientIdSecret } from "../component/ClientIdSecret";
import { DisplayOrder } from "../component/DisplayOrder";

type GeneralSettingsProps = {
  id: string;
  showClientIdSecret?: boolean;
  create?: boolean;
};

export const GeneralSettings = ({
  create = true,
  showClientIdSecret = true,
  id,
}: GeneralSettingsProps) => {
  return (
    <>
      <RedirectUrl id={id} />
      {showClientIdSecret && <ClientIdSecret create={create} />}
      <DisplayOrder />
    </>
  );
};
