import { useTranslation } from "react-i18next";
import { PageSection } from "@patternfly/react-core";

import type { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";

type ForbiddenSectionProps = {
  permissionNeeded: AccessType | AccessType[];
};

export const ForbiddenSection = ({
  permissionNeeded,
}: ForbiddenSectionProps) => {
  const { t } = useTranslation();
  const permissionNeededArray = Array.isArray(permissionNeeded)
    ? permissionNeeded
    : [permissionNeeded];

  return (
    <PageSection>
      {t("forbidden", { count: permissionNeededArray.length })}{" "}
      {permissionNeededArray.map((p) => p.toString()).join(", ")}
    </PageSection>
  );
};
