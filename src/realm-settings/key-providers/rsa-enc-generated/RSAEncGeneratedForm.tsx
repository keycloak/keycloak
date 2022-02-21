import React from "react";
import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useRouteMatch } from "react-router-dom";

import { ViewHeader } from "../../../components/view-header/ViewHeader";
import {
  MatchParams,
  RSAGeneratedForm,
} from "../rsa-generated/RSAGeneratedForm";

export default function RSAEncGeneratedSettings() {
  const { t } = useTranslation("realm-settings");
  const providerId = useRouteMatch<MatchParams>(
    "/:realm/realm-settings/keys/:id?/:providerType?/settings"
  )?.params.providerType;

  return (
    <>
      <ViewHeader titleKey={t("editProvider")} subKey={providerId} />
      <PageSection variant="light">
        <RSAGeneratedForm providerType={providerId} editMode />
      </PageSection>
    </>
  );
}
