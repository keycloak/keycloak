import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import SessionsTable from "../sessions/SessionsTable";
import { useParams } from "../utils/useParams";
import type { UserParams } from "./routes/User";

export const UserSessions = () => {
  const { adminClient } = useAdminClient();

  const { id } = useParams<UserParams>();
  const { realm } = useRealm();
  const { t } = useTranslation();

  const loader = () => adminClient.users.listSessions({ id, realm });

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <SessionsTable
        loader={loader}
        hiddenColumns={["username", "type"]}
        emptyInstructions={t("noSessionsForUser")}
        logoutUser={id}
      />
    </PageSection>
  );
};
