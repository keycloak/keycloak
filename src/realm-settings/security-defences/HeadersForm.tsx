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
        url="http://tools.ietf.org/html/rfc7034"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.contentSecurityPolicy"
        url="http://www.w3.org/TR/CSP/"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.contentSecurityPolicyReportOnly"
        url="http://www.w3.org/TR/CSP/"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xContentTypeOptions"
        url="https://www.owasp.org/index.php/List_of_useful_HTTP_headers"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xRobotsTag"
        url="https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xXSSProtection"
        url="https://www.owasp.org/index.php/OWASP_Secure_Headers_Project#xxxsp"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.strictTransportSecurity"
        url="https://www.owasp.org/index.php/OWASP_Secure_Headers_Project#hsts"
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
