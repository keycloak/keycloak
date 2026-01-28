import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  ErrorBoundaryProvider,
  KeycloakSpinner,
  ListEmptyState,
  OrganizationTable,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { getUserOrganizations } from "../api/methods";
import { Page } from "../components/page/Page";
import { Environment } from "../environment";
import { usePromise } from "../utils/usePromise";

export const Organizations = () => {
  const { t } = useTranslation();
  const context = useEnvironment<Environment>();

  const [userOrgs, setUserOrgs] = useState<OrganizationRepresentation[]>([]);

  usePromise(
    (signal) => getUserOrganizations({ signal, context }),
    setUserOrgs,
  );

  if (!userOrgs) {
    return <KeycloakSpinner />;
  }

  return (
    <Page title={t("organizations")} description={t("organizationDescription")}>
      <ErrorBoundaryProvider>
        <OrganizationTable
          link={({ children }) => <span>{children}</span>}
          loader={userOrgs}
        >
          <ListEmptyState
            message={t("emptyUserOrganizations")}
            instructions={t("emptyUserOrganizationsInstructions")}
          />
        </OrganizationTable>
      </ErrorBoundaryProvider>
    </Page>
  );
};

export default Organizations;
