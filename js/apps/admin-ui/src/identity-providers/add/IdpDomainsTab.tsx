import type OrganizationDomainRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationDomainRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import { PageSection } from "@patternfly/react-core";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toEditOrganization } from "../../organizations/routes/EditOrganization";

type DomainWithOrg = OrganizationDomainRepresentation & {
  orgId: string;
  orgName: string;
};

type IdpDomainsTabProps = {
  alias: string;
};

export const IdpDomainsTab = ({ alias }: IdpDomainsTabProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm } = useRealm();

  const [key, setKey] = useState(0);

  const loader = async (): Promise<DomainWithOrg[]> => {
    const idp = await adminClient.identityProviders.findOne({ alias });
    const links = idp?.organizationLinks ?? [];

    const results = await Promise.all(
      links.map(async (link) => {
        try {
          const org = await adminClient.organizations.findOne({
            id: link.organizationId!,
          });
          if (!org) return [];
          return (org.domains ?? [])
            .filter((d) => d.identityProviderAlias === alias)
            .map((d) => ({
              ...d,
              orgId: org.id!,
              orgName: org.name!,
            }));
        } catch {
          return [];
        }
      }),
    );
    return results.flat();
  };

  return (
    <PageSection variant="light">
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="domains"
        searchPlaceholderKey="searchDomain"
        columns={[
          {
            name: "name",
            displayKey: "domain",
          },
          {
            name: "orgName",
            displayKey: "organization",
            cellRenderer: (row) => (
              <Link
                to={toEditOrganization({
                  realm,
                  id: row.orgId,
                  tab: "domains",
                })}
              >
                {row.orgName} <ExternalLinkAltIcon />
              </Link>
            ),
          },
          {
            name: "autoRedirect",
            displayKey: "autoRedirect",
            cellRenderer: (row) =>
              row.autoRedirect ? t("yes") : t("no"),
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noLinkedDomains")}
            instructions={t("noLinkedDomainsInstructions")}
          />
        }
      />
    </PageSection>
  );
};
