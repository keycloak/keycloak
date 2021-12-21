import React from "react";
import { PageSection } from "@patternfly/react-core";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { RolesList } from "./RolesList";
import helpUrls from "../help-urls";

export default function RealmRolesSection() {
  const adminClient = useAdminClient();

  const loader = (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };

    const searchParam = search || "";

    if (searchParam) {
      params.search = searchParam;
    }

    return adminClient.roles.find(params);
  };

  return (
    <>
      <ViewHeader
        titleKey="roles:title"
        subKey="roles:roleExplain"
        helpUrl={helpUrls.realmRolesUrl}
      />
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <RolesList loader={loader} />
      </PageSection>
    </>
  );
}
