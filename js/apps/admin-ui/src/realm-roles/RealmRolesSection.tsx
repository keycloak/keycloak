import { PageSection } from "@patternfly/react-core";
import { useAdminClient } from "../admin-client";
import { RolesList } from "../components/roles-list/RolesList";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import helpUrls from "../help-urls";
import { toAddRole } from "./routes/AddRole";
import { toRealmRole } from "./routes/RealmRole";

export default function RealmRolesSection() {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-realm");

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
        titleKey="titleRoles"
        subKey="roleExplain"
        helpUrl={helpUrls.realmRolesUrl}
      />
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <RolesList
          loader={loader}
          toCreate={toAddRole({ realm })}
          toDetail={(roleId) =>
            toRealmRole({ realm, id: roleId, tab: "details" })
          }
          isReadOnly={!isManager}
        />
      </PageSection>
    </>
  );
}
