import React from "react";
import { PageSection } from "@patternfly/react-core";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { RolesList } from "./RolesList";

export const RealmRolesSection = () => {
  const adminClient = useAdminClient();
  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
      search: search!,
    };
    return await adminClient.roles.find(params);
  };
  return (
    <>
      <ViewHeader titleKey="roles:title" subKey="roles:roleExplain" />
      <PageSection variant="light">
        <RolesList loader={loader} />
      </PageSection>
    </>
  );
};
