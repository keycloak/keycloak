import React, { useState } from "react";
import { Link, useRouteMatch } from "react-router-dom";
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
} from "@patternfly/react-core";
import { CheckCircleIcon } from "@patternfly/react-icons";

import type AuthenticationFlowRepresentation from "keycloak-admin/lib/defs/authenticationFlowRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAlerts } from "../components/alert/Alerts";
import { toUpperCase } from "../util";
import { DuplicateFlowModal } from "./DuplicateFlowModal";

import "./authentication-section.css";

type UsedBy = "client" | "default" | "idp";

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

export const AuthenticationSection = () => {
  const { t } = useTranslation("authentication");
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const { addAlert } = useAlerts();
  const { url } = useRouteMatch();

  const [selectedFlow, setSelectedFlow] = useState<AuthenticationType>();
  const [open, setOpen] = useState(false);

  const loader = async () => {
    const clients = await adminClient.clients.find();
    const idps = await adminClient.identityProviders.find();
    const realmRep = await adminClient.realms.findOne({ realm });
    const defaultFlows = Object.entries(realmRep)
      .filter((entry) => realmFlows.includes(entry[0]))
      .map((entry) => entry[1]);

    const flows = (await adminClient.authenticationManagement.getFlows()) as AuthenticationType[];
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
        flow.usedBy.type = "client";
        flow.usedBy.values.push(client.clientId!);
      }

      const idp = idps.find(
        (idp) =>
          idp.firstBrokerLoginFlowAlias === flow.alias ||
          idp.postBrokerLoginFlowAlias === flow.alias
      );
      if (idp) {
        flow.usedBy.type = "idp";
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
        addAlert(t("deleteFlowError", { error }), AlertVariant.danger);
      }
    },
  });

  const UsedBy = ({ id, usedBy: { type, values } }: AuthenticationType) => (
    <>
      {(type === "idp" || type === "client") && (
        <Popover
          key={id}
          aria-label="Basic popover"
          bodyContent={
            <div key={`usedBy-${id}-${values}`}>
              {t("appliedBy" + (type === "client" ? "Clients" : "Providers"))}{" "}
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
            {t("specific" + (type === "client" ? "Clients" : "Providers"))}
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

  const AliasRenderer = ({ id, alias, builtIn }: AuthenticationType) => (
    <>
      <Link to={`${url}/${id}`} key={`link-{id}`}>
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
          toggleDialog={() => setOpen(!open)}
          onComplete={() => {
            refresh();
            setOpen(false);
          }}
        />
      )}
      <ViewHeader titleKey="authentication:title" divider={false} />
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
              searchPlaceholderKey="authentication:searchForEvent"
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
        </KeycloakTabs>
      </PageSection>
    </>
  );
};
