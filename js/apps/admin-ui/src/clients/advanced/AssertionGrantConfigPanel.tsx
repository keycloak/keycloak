import { useTranslation } from "react-i18next";
import { ActionGroup, Button } from "@patternfly/react-core";

import { FormAccess } from "../../components/form/FormAccess";
import { convertAttributeNameToForm } from "../../util";
import type { AdvancedProps } from "../AdvancedTab";
import { CrossDomainSelect } from "../../components/cross-domain/crossDomainSelect";

export const AssertionGrantConfigPanel = ({
  save,
  client: { access, protocol },
}: AdvancedProps) => {
  const { t } = useTranslation();

  return (
    <FormAccess
      role="manage-clients"
      fineGrainedAccess={access?.configure}
      isHorizontal
    >
      {protocol === "openid-connect" && (
        <>
          <CrossDomainSelect
            name={convertAttributeNameToForm(
              "attributes.oidc.grants.assertion.config",
            )}
            label={t("oidcClientJWTBearerCrossDomainTitle")}
            helpText={t("oidcClientJWTBearerCrossDomainHelp")}
            onChange={(d) => JSON.stringify(d.map((c) => c.issuer))}
            onLoad={(trustedDomains, data) => {
              const loaded = JSON.parse(data || "[]");
              return trustedDomains.filter((c) => loaded.includes(c.issuer));
            }}
          />
          <ActionGroup>
            <Button variant="secondary" onClick={() => save()}>
              {t("save")}
            </Button>
          </ActionGroup>
        </>
      )}
    </FormAccess>
  );
};
