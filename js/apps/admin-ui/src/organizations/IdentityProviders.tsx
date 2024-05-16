import { Button, PageSection, ToolbarItem } from "@patternfly/react-core";
import { BellIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useFetch } from "../utils/useFetch";
import { EditOrganizationParams } from "./routes/EditOrganization";

export const IdentityProviders = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { id: orgId } = useParams<EditOrganizationParams>();

  // const [key, setKey] = useState(0);
  // const refresh = () => setKey(key + 1);

  const [hasProviders, setHasProviders] = useState(false);

  useFetch(
    async () => adminClient.identityProviders.find({ max: 1 }),
    (providers) => {
      setHasProviders(providers.length === 1);
    },
    [],
  );

  const loader = () =>
    adminClient.organizations.listIdentityProviders({ orgId: orgId! });

  return (
    <PageSection variant="light">
      {!hasProviders ? (
        <ListEmptyState
          icon={BellIcon}
          message={t("noIdentityProvider")}
          instructions={t("noIdentityProviderInstructions")}
        />
      ) : (
        <KeycloakDataTable
          // key={key}
          loader={loader}
          isPaginated
          ariaLabelKey="identityProviders"
          searchPlaceholderKey="searchProvider"
          toolbarItem={
            <ToolbarItem>
              <Button>{t("linkIdentityProvider")}</Button>
            </ToolbarItem>
          }
          actions={[
            {
              title: t("edit"),
              onRowClick: async () => {
                console.log("click");
              },
            },
            {
              title: t("unLinkIdentityProvider"),
              onRowClick: async () => {
                console.log("click");
              },
            },
          ]}
          columns={[
            {
              name: "alias",
            },
            {
              name: "config['kc.org.domain']",
              displayKey: "domain",
            },
            {
              name: "providerId",
              displayKey: "providerDetails",
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("emptyIdentityProviderLink")}
              instructions={t("emptyIdentityProviderLinkInstructions")}
              primaryActionText={t("linkIdentityProvider")}
            />
          }
        />
      )}
    </PageSection>
  );
};
