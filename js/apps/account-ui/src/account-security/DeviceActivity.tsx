import {
  ContinueCancelModal,
  useEnvironment,
  label,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  DataList,
  DataListContent,
  DataListItem,
  DataListItemRow,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Label,
  Spinner,
  Split,
  SplitItem,
  Title,
} from "@patternfly/react-core";
import {
  DesktopIcon,
  MobileAltIcon,
  SyncAltIcon,
} from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { deleteSession, getDevices } from "../api/methods";
import {
  ClientRepresentation,
  DeviceRepresentation,
  SessionRepresentation,
} from "../api/representations";
import { Page } from "../components/page/Page";
import { formatDate } from "../utils/formatDate";
import { useAccountAlerts } from "../utils/useAccountAlerts";
import { usePromise } from "../utils/usePromise";

export const DeviceActivity = () => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();

  const [devices, setDevices] = useState<DeviceRepresentation[]>();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const moveCurrentToTop = (devices: DeviceRepresentation[]) => {
    let currentDevice = devices[0];

    const index = devices.findIndex((d) => d.current);
    currentDevice = devices.splice(index, 1)[0];
    devices.unshift(currentDevice);

    const sessionIndex = currentDevice.sessions.findIndex((s) => s.current);
    const currentSession = currentDevice.sessions.splice(sessionIndex, 1)[0];
    currentDevice.sessions.unshift(currentSession);

    setDevices(devices);
  };

  usePromise((signal) => getDevices({ signal, context }), moveCurrentToTop, [
    key,
  ]);

  const signOutAll = async () => {
    await deleteSession(context);
    await context.keycloak.logout();
  };

  const signOutSession = async (
    session: SessionRepresentation,
    device: DeviceRepresentation,
  ) => {
    try {
      await deleteSession(context, session.id);
      addAlert(
        t("signedOutSession", { browser: session.browser, os: device.os }),
      );
      refresh();
    } catch (error) {
      addError("errorSignOutMessage", error);
    }
  };

  const makeClientsString = (clients: ClientRepresentation[]): string => {
    let clientsString = "";
    clients.forEach((client, index) => {
      let clientName: string;
      if (client.clientName !== "") {
        clientName = label(t, client.clientName);
      } else {
        clientName = client.clientId;
      }

      clientsString += clientName;

      if (clients.length > index + 1) clientsString += ", ";
    });

    return clientsString;
  };

  if (!devices) {
    return <Spinner />;
  }

  return (
    <Page
      title={t("deviceActivity")}
      description={t("signedInDevicesExplanation")}
    >
      <Split hasGutter className="pf-v5-u-mb-lg">
        <SplitItem isFilled>
          <Title headingLevel="h2" size="xl">
            {t("signedInDevices")}
          </Title>
        </SplitItem>
        <SplitItem>
          <Button
            id="refresh-page"
            variant="link"
            onClick={() => refresh()}
            icon={<SyncAltIcon />}
          >
            {t("refreshPage")}
          </Button>

          {(devices.length > 1 || devices[0].sessions.length > 1) && (
            <ContinueCancelModal
              buttonTitle={t("signOutAllDevices")}
              modalTitle={t("signOutAllDevices")}
              continueLabel={t("confirm")}
              cancelLabel={t("cancel")}
              onContinue={() => signOutAll()}
            >
              {t("signOutAllDevicesWarning")}
            </ContinueCancelModal>
          )}
        </SplitItem>
      </Split>
      <DataList
        className="signed-in-device-list"
        aria-label={t("signedInDevices")}
      >
        <DataListItem aria-labelledby={`sessions-${key}`}>
          {devices.map((device) =>
            device.sessions.map((session, index) => (
              <DataListItemRow key={device.id} data-testid={`row-${index}`}>
                <DataListContent
                  aria-label="device-sessions-content"
                  className="pf-v5-u-flex-grow-1"
                >
                  <Grid hasGutter>
                    <GridItem span={1} rowSpan={2}>
                      {device.mobile ? <MobileAltIcon /> : <DesktopIcon />}
                    </GridItem>
                    <GridItem sm={8} md={9} span={10}>
                      <span className="pf-v5-u-mr-md session-title">
                        {device.os.toLowerCase().includes("unknown")
                          ? t("unknownOperatingSystem")
                          : device.os}{" "}
                        {!device.osVersion.toLowerCase().includes("unknown") &&
                          device.osVersion}{" "}
                        / {session.browser}
                      </span>
                      {session.current && (
                        <Label color="green">{t("currentSession")}</Label>
                      )}
                    </GridItem>
                    <GridItem
                      className="pf-v5-u-text-align-right"
                      sm={3}
                      md={2}
                      span={1}
                    >
                      {!session.current && (
                        <ContinueCancelModal
                          buttonTitle={t("signOut")}
                          modalTitle={t("signOut")}
                          continueLabel={t("confirm")}
                          cancelLabel={t("cancel")}
                          buttonVariant="secondary"
                          onContinue={() => signOutSession(session, device)}
                        >
                          {t("signOutWarning")}
                        </ContinueCancelModal>
                      )}
                    </GridItem>
                    <GridItem span={11}>
                      <DescriptionList
                        className="signed-in-device-grid"
                        columnModifier={{ sm: "2Col", lg: "3Col" }}
                        cols={5}
                        rows={1}
                      >
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("ipAddress")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {session.ipAddress}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("lastAccessedOn")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {formatDate(new Date(session.lastAccess * 1000))}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("clients")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {makeClientsString(session.clients)}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("started")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {formatDate(new Date(session.started * 1000))}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                        <DescriptionListGroup>
                          <DescriptionListTerm>
                            {t("expires")}
                          </DescriptionListTerm>
                          <DescriptionListDescription>
                            {formatDate(new Date(session.expires * 1000))}
                          </DescriptionListDescription>
                        </DescriptionListGroup>
                      </DescriptionList>
                    </GridItem>
                  </Grid>
                </DataListContent>
              </DataListItemRow>
            )),
          )}
        </DataListItem>
      </DataList>
    </Page>
  );
};

export default DeviceActivity;
