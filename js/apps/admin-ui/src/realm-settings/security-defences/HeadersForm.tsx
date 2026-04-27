import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { ActionGroup, Button } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { HelpLinkTextInput } from "./HelpLinkTextInput";

import "./security-defences.css";

type HeadersFormProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const HeadersForm = ({ realm, save }: HeadersFormProps) => {
  const { t } = useTranslation();
  const form = useFormContext<RealmRepresentation>();
  const {
    reset,
    formState: { isDirty },
    handleSubmit,
  } = form;

  return (
    <FormAccess
      isHorizontal
      role="manage-realm"
      className="keycloak__security-defences__form"
      onSubmit={handleSubmit(save)}
    >
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xFrameOptions"
        url="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.contentSecurityPolicy"
        url="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.contentSecurityPolicyReportOnly"
        url="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xContentTypeOptions"
        url="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.xRobotsTag"
        url="https://developers.google.com/search/docs/advanced/robots/robots_meta_tag"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.strictTransportSecurity"
        url="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security"
      />
      <HelpLinkTextInput
        fieldName="browserSecurityHeaders.referrerPolicy"
        url="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy"
      />

      <ActionGroup>
        <Button
          variant="primary"
          type="submit"
          data-testid="headers-form-tab-save"
          isDisabled={!isDirty}
        >
          {t("save")}
        </Button>
        <Button variant="link" onClick={() => reset(realm)}>
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
