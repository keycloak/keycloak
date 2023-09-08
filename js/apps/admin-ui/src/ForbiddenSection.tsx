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
  const count = Array.isArray(permissionNeeded) ? permissionNeeded.length : 1;

  return (
    <PageSection>
      {t("forbidden", { count })} {permissionNeeded}
    </PageSection>
  );
};
