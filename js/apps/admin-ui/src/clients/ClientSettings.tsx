import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { Form } from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { ClientDescription } from "./ClientDescription";
import { CapabilityConfig } from "./add/CapabilityConfig";
import { SamlConfig } from "./add/SamlConfig";
import { SamlSignature } from "./add/SamlSignature";
import { AccessSettings } from "./add/AccessSettings";
import { LoginSettingsPanel } from "./add/LoginSettingsPanel";
import { LogoutPanel } from "./add/LogoutPanel";
import { FormFields } from "./ClientDetails";

export type ClientSettingsProps = {
  client: ClientRepresentation;
  save: () => void;
  reset: () => void;
};

export const ClientSettings = (props: ClientSettingsProps) => {
  const { t } = useTranslation("clients");

  const { watch } = useFormContext<FormFields>();
  const protocol = watch("protocol");

  const { client } = props;

  return (
    <ScrollForm
      className="pf-u-px-lg pf-u-pb-lg"
      sections={[
        {
          title: t("generalSettings"),
          panel: (
            <Form isHorizontal>
              <ClientDescription
                protocol={client.protocol}
                hasConfigureAccess={client.access?.configure}
              />
            </Form>
          ),
        },
        {
          title: t("accessSettings"),
          panel: <AccessSettings {...props} />,
        },
        {
          title: t("samlCapabilityConfig"),
          isHidden: protocol !== "saml" || client.bearerOnly,
          panel: <SamlConfig />,
        },
        {
          title: t("signatureAndEncryption"),
          isHidden: protocol !== "saml" || client.bearerOnly,
          panel: <SamlSignature />,
        },
        {
          title: t("capabilityConfig"),
          isHidden: protocol !== "openid-connect" || client.bearerOnly,
          panel: <CapabilityConfig />,
        },
        {
          title: t("loginSettings"),
          isHidden: client.bearerOnly,
          panel: <LoginSettingsPanel access={client.access?.configure} />,
        },
        {
          title: t("logoutSettings"),
          isHidden: client.bearerOnly,
          panel: <LogoutPanel {...props} />,
        },
      ]}
    />
  );
};
