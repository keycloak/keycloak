import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Label,
  PageSection,
  Popover,
  Tab,
  TabTitleText,
  ToolbarItem,
} from "@patternfly/react-core";
import { CheckCircleIcon } from "@patternfly/react-icons";

import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";
import { toUpperCase } from "../util";
import useToggle from "../utils/useToggle";
import { DuplicateFlowModal } from "./DuplicateFlowModal";
import { toCreateFlow } from "./routes/CreateFlow";
import { toFlow } from "./routes/Flow";
import { RequiredActions } from "./RequiredActions";
import { Policies } from "./policies/Policies";

import "./authentication-section.css";
import helpUrls from "../help-urls";

type UsedBy = "specificClients" | "default" | "specificProviders";

type AuthenticationType = AuthenticationFlowRepresentation & {
  usedBy: { type?: UsedBy; values: string[] };
};

const realmFlows = [
  "browserFlow",
  "registrationFlow",
  "directGrantFlow",
  "resetCredentialsFlow",
  "clientAuthenticationFlow",
  "dockerAuthenticationFlow",
];

export default function AuthenticationSection() {
  const { t } = useTranslation("authentication");
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const { addAlert, addError } = useAlerts();

  const [selectedFlow, setSelectedFlow] = useState<AuthenticationType>();
  const [open, toggleOpen, setOpen] = useToggle();

  const loader = async () => {
    const clients = await adminClient.clients.find();
    const idps = await adminClient.identityProviders.find();
    const realmRep = await adminClient.realms.findOne({ realm });
    if (!realmRep) {
      throw new Error(t("common:notFound"));
    }

    const defaultFlows = Object.entries(realmRep)
      .filter((entry) => realmFlows.includes(entry[0]))
      .map((entry) => entry[1]);

    const flows =
      (await adminClient.authenticationManagement.getFlows()) as AuthenticationType[];
    for (const flow of flows) {
      flow.usedBy = { values: [] };
      const client = clients.find(
        (client) =>
          client.authenticationFlowBindingOverrides &&
          (client.authenticationFlowBindingOverrides["direct_grant"] ===
            flow.id ||
            client.authenticationFlowBindingOverrides["browser"] === flow.id)
      );
      if (client) {
        flow.usedBy.type = "specificClients";
        flow.usedBy.values.push(client.clientId!);
      }

      const idp = idps.find(
        (idp) =>
          idp.firstBrokerLoginFlowAlias === flow.alias ||
          idp.postBrokerLoginFlowAlias === flow.alias
      );
      if (idp) {
        flow.usedBy.type = "specificProviders";
        flow.usedBy.values.push(idp.alias!);
      }

      const isDefault = defaultFlows.includes(flow.alias);
      if (isDefault) {
        flow.usedBy.type = "default";
      }
    }

    return flows;
  };
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

  const UsedBy = ({ id, usedBy: { type, values } }: AuthenticationType) => (
    <>
      {(type === "specificProviders" || type === "specificClients") && (
        <Popover
          key={id}
          aria-label={t("usedBy")}
          bodyContent={
            <div key={`usedBy-${id}-${values}`}>
              {t(
                "appliedBy" +
                  (type === "specificClients" ? "Clients" : "Providers")
              )}{" "}
              {values.map((used, index) => (
                <>
                  <strong>{used}</strong>
                  {index < values.length - 1 ? ", " : ""}
                </>
              ))}
            </div>
          }
        >
          <Button variant={ButtonVariant.link} key={`button-${id}`}>
            <CheckCircleIcon
              className="keycloak_authentication-section__usedby"
              key={`icon-${id}`}
            />{" "}
            {t(type)}
          </Button>
        </Popover>
      )}
      {type === "default" && (
        <Button key={id} variant={ButtonVariant.link} isDisabled>
          <CheckCircleIcon
            className="keycloak_authentication-section__usedby"
            key={`icon-${id}`}
          />{" "}
          {t("default")}
        </Button>
      )}
      {!type && (
        <Button key={id} variant={ButtonVariant.link} isDisabled>
          {t("notInUse")}
        </Button>
      )}
    </>
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
          usedBy: usedBy.type || "notInUse",
          builtIn: builtIn ? "builtIn" : undefined,
        })}
        key={`link-${id}`}
      >
        {toUpperCase(alias!)}
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
            setOpen(false);
          }}
        />
      )}
      <ViewHeader
        titleKey="authentication:title"
        subKey="authentication:authenticationExplain"
        helpUrl={helpUrls.authenticationUrl}
        divider={false}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakTabs isBox>
          <Tab
            eventKey="flows"
            title={<TabTitleText>{t("flows")}</TabTitleText>}
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
              actionResolver={({ data }) => {
                const defaultActions = [
                  {
                    title: t("duplicate"),
                    onClick: () => {
                      setOpen(true);
                      setSelectedFlow(data);
                    },
                  },
                ];
                // remove delete when it's in use or default flow
                if (data.builtIn || data.usedBy.values.length > 0) {
                  return defaultActions;
                } else {
                  return [
                    {
                      title: t("common:delete"),
                      onClick: () => {
                        setSelectedFlow(data);
                        toggleDeleteDialog();
                      },
                    },
                    ...defaultActions,
                  ];
                }
              }}
              columns={[
                {
                  name: "alias",
                  displayKey: "authentication:flowName",
                  cellRenderer: AliasRenderer,
                },
                {
                  name: "usedBy",
                  displayKey: "authentication:usedBy",
                  cellRenderer: UsedBy,
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
            id="requiredActions"
            eventKey="requiredActions"
            title={<TabTitleText>{t("requiredActions")}</TabTitleText>}
          >
            <RequiredActions />
          </Tab>
          <Tab
            id="policies"
            eventKey="policies"
            title={<TabTitleText>{t("policies")}</TabTitleText>}
          >
            <Policies />
          </Tab>
        </KeycloakTabs>
      </PageSection>
    </>
  );
}
