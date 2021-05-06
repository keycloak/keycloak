import React from "react";

import { RedirectUrl } from "../component/RedirectUrl";
import { ClientIdSecret } from "../component/ClientIdSecret";
import { DisplayOrder } from "../component/DisplayOrder";

export const GeneralSettings = () => (
  <>
    <RedirectUrl />
    <ClientIdSecret />
    <DisplayOrder />
  </>
);
