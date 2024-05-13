import { Chip, ChipGroup, PageSection } from "@patternfly/react-core";
// import { useState } from "react";
import { useTranslation } from "react-i18next";
// import { useAdminClient } from "../admin-client";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../components/view-header/ViewHeader";

const Domains = (org: any) => {
  const { t } = useTranslation();
  return (
    <ChipGroup
      numChips={1}
      expandedText={t("hide")}
      collapsedText={t("showRemaining")}
    >
      {org.domains.map((o: string) => (
        <Chip key={o} isReadOnly>
          {o}
        </Chip>
      ))}
    </ChipGroup>
  );
};

export default function OrganizationSection() {
  // const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  // const [key, setKey] = useState(0);

  function loader() {
    return Promise.resolve([
      { name: "test", domains: ["one.ch", "two.ch"], description: "my domain" },
    ]);
  }
  return (
    <>
      <ViewHeader
        titleKey="organizationsList"
        subKey="organizationsExplain"
        divider
      />
      <PageSection variant="light">
        <KeycloakDataTable
          loader={loader}
          isPaginated
          ariaLabelKey="organizationList"
          searchPlaceholderKey="searchOrganization"
          columns={[
            {
              name: "name",
              displayKey: "name",
            },
            {
              name: "domains",
              displayKey: "domains",
              cellRenderer: Domains,
            },
            {
              name: "description",
              displayKey: "description",
            },
          ]}
          emptyState={
            <ListEmptyState
              message={t("emptyOrganizations")}
              instructions={t("emptyOrganizationsInstructions")}
            />
          }
        />
      </PageSection>
    </>
  );
}
