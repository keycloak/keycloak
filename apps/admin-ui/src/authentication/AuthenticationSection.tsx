import { useState } from "react";
import { Link } from "react-router-dom-v5-compat";
import { Trans, useTranslation } from "react-i18next";
import { sortBy } from "lodash-es";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Label,
  PageSection,
  Tab,
  TabTitleText,
  ToolbarItem,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";
import useToggle from "../utils/useToggle";
import { DuplicateFlowModal } from "./DuplicateFlowModal";
import { toCreateFlow } from "./routes/CreateFlow";
import { toFlow } from "./routes/Flow";
import { RequiredActions } from "./RequiredActions";
import { Policies } from "./policies/Policies";
import helpUrls from "../help-urls";
import { BindFlowDialog } from "./BindFlowDialog";
import { UsedBy } from "./components/UsedBy";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { AuthenticationTab, toAuthentication } from "./routes/Authentication";
import { addTrailingSlash } from "../util";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";
import useLocaleSort, { mapByKey } from "../utils/useLocaleSort";

import "./authentication-section.css";

type UsedBy = "SPECIFIC_CLIENTS" | "SPECIFIC_PROVIDERS" | "DEFAULT";

export type AuthenticationType = AuthenticationFlowRepresentation & {
  usedBy?: { type?: UsedBy; values: string[] };
};

export const REALM_FLOWS = new Map<string, string>([
  ["browserFlow", "browser"],
  ["registrationFlow", "registration"],
  ["directGrantFlow", "direct grant"],
  ["resetCredentialsFlow", "reset credentials"],
  ["clientAuthenticationFlow", "clients"],
  ["dockerAuthenticationFlow", "docker auth"],
]);

export default function AuthenticationSection() {
  const { t } = useTranslation("authentication");
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const { addAlert, addError } = useAlerts();
  const localeSort = useLocaleSort();
  const [selectedFlow, setSelectedFlow] = useState<AuthenticationType>();
  const [open, toggleOpen] = useToggle();
  const [bindFlowOpen, toggleBindFlow] = useToggle();

  const loader = async () => {
    const flowsRequest = await fetch(
      `${addTrailingSlash(
        adminClient.baseUrl
      )}admin/realms/${realm}/admin-ui-authentication-management/flows`,
      {
        method: "GET",
        headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
      }
    );
    const flows = await flowsRequest.json();

    if (!flows) {
      return [];
    }

    return sortBy(
      localeSort<AuthenticationType>(flows, mapByKey("alias")),
      (flow) => flow.usedBy?.type
    );
  };

  const useTab = (tab: AuthenticationTab) =>
    useRoutableTab(toAuthentication({ realm, tab }));

  const flowsTab = useTab("flows");
  const requiredActionsTab = useTab("required-actions");
  const policiesTab = useTab("policies");

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "authentication:deleteConfirmFlow",
    children: (
      <Trans i18nKey="authentication:deleteConfirmFlowMessage">
        {" "}
        <strong>{{ flow: selectedFlow ? selectedFlow.alias : "" }}</strong>.
      </Trans>
    ),
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.authenticationManagement.deleteFlow({
          flowId: selectedFlow!.id!,
        });
        refresh();
        addAlert(t("deleteFlowSuccess"), AlertVariant.success);
      } catch (error) {
        addError("authentication:deleteFlowError", error);
      }
    },
  });

  const UsedByRenderer = (authType: AuthenticationType) => (
    <UsedBy authType={authType} />
  );

  const AliasRenderer = ({
    id,
    alias,
    usedBy,
    builtIn,
  }: AuthenticationType) => (
    <>
      <Link
        to={toFlow({
          realm,
          id: id!,
          usedBy: usedBy?.type || "notInUse",
          builtIn: builtIn ? "builtIn" : undefined,
        })}
        key={`link-${id}`}
      >
        {alias}
      </Link>{" "}
      {builtIn && <Label key={`label-${id}`}>{t("buildIn")}</Label>}
    </>
  );

  return (
    <>
      <DeleteConfirm />
      {open && (
        <DuplicateFlowModal
          name={selectedFlow ? selectedFlow.alias! : ""}
          description={selectedFlow?.description!}
          toggleDialog={toggleOpen}
          onComplete={() => {
            refresh();
            toggleOpen();
          }}
        />
      )}
      {bindFlowOpen && (
        <BindFlowDialog
          onClose={() => {
            toggleBindFlow();
            refresh();
          }}
          flowAlias={selectedFlow?.alias!}
        />
      )}
      <ViewHeader
        titleKey="authentication:title"
        subKey="authentication:authenticationExplain"
        helpUrl={helpUrls.authenticationUrl}
        divider={false}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs
          isBox
          defaultLocation={toAuthentication({ realm, tab: "flows" })}
        >
          <Tab
            data-testid="flows"
            title={<TabTitleText>{t("flows")}</TabTitleText>}
            {...flowsTab}
          >
            <KeycloakDataTable
              key={key}
              loader={loader}
              ariaLabelKey="authentication:title"
              searchPlaceholderKey="authentication:searchForFlow"
              toolbarItem={
                <ToolbarItem>
                  <Button
                    component={(props) => (
                      <Link {...props} to={toCreateFlow({ realm })} />
                    )}
                  >
                    {t("createFlow")}
                  </Button>
                </ToolbarItem>
              }
              actionResolver={({ data }) => [
                {
                  title: t("duplicate"),
                  onClick: () => {
                    toggleOpen();
                    setSelectedFlow(data);
                  },
                },
                ...(data.usedBy?.type !== "DEFAULT"
                  ? [
                      {
                        title: t("bindFlow"),
                        onClick: () => {
                          toggleBindFlow();
                          setSelectedFlow(data);
                        },
                      },
                    ]
                  : []),
                ...(!data.builtIn && !data.usedBy
                  ? [
                      {
                        title: t("common:delete"),
                        onClick: () => {
                          setSelectedFlow(data);
                          toggleDeleteDialog();
                        },
                      },
                    ]
                  : []),
              ]}
              columns={[
                {
                  name: "alias",
                  displayKey: "authentication:flowName",
                  cellRenderer: AliasRenderer,
                },
                {
                  name: "usedBy",
                  displayKey: "authentication:usedBy",
                  cellRenderer: UsedByRenderer,
                },
                {
                  name: "description",
                  displayKey: "common:description",
                },
              ]}
              emptyState={
                <ListEmptyState
                  message={t("emptyEvents")}
                  instructions={t("emptyEventsInstructions")}
                />
              }
            />
          </Tab>
          <Tab
            data-testid="requiredActions"
            title={<TabTitleText>{t("requiredActions")}</TabTitleText>}
            {...requiredActionsTab}
          >
            <RequiredActions />
          </Tab>
          <Tab
            data-testid="policies"
            title={<TabTitleText>{t("policies")}</TabTitleText>}
            {...policiesTab}
          >
            <Policies />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
