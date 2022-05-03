import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { ActionGroup, Button } from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpLinkTextInput } from "./HelpLinkTextInput";

import "./security-defences.css";

type HeadersFormProps = {
  save: (realm: RealmRepresentation) => void;
  reset: () => void;
};

export const HeadersForm = ({ save, reset }: HeadersFormProps) => {
  const { t } = useTranslation();
  const {
    formState: { isDirty },
    handleSubmit,
  } = useFormContext();

  return (
    <FormAccess
      isHorizontal
      role="manage-realm"
      className="keycloak__security-defences__form"
      onSubmit={handleSubmit(save)}
    >
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xFrameOptions"
        url="https://datatracker.ietf.org/doc/html/rfc7034"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.contentSecurityPolicy"
        url="https://www.w3.org/TR/CSP/"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.contentSecurityPolicyReportOnly"
        url="https://www.w3.org/TR/CSP/"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xContentTypeOptions"
        url="https://owasp.org/index.php/List_of_useful_HTTP_headers"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xRobotsTag"
        url="https://developers.google.com/search/docs/advanced/robots/robots_meta_tag"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xXSSProtection"
        url="https://owasp.org/www-project-secure-headers/#xxxsp"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.strictTransportSecurity"
        url="https://owasp.org/www-project-secure-headers/#hsts"
      />

      <ActionGroup>
        <Button
          variant="primary"
          type="submit"
          data-testid="headers-form-tab-save"
          isDisabled={!isDirty}
        >
          {t("common:save")}
        </Button>
        <Button variant="link" onClick={reset}>
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
