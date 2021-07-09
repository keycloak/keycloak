import {
  Brand,
  Button,
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  EmptyState,
  EmptyStateBody,
  Grid,
  GridItem,
  Label,
  List,
  ListItem,
  ListVariant,
  PageSection,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";
import React from "react";
import { useTranslation } from "react-i18next";
import _ from "lodash";

import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";

import "./dashboard.css";
import { toUpperCase } from "../util";
import { HelpItem } from "../components/help-enabler/HelpItem";

const EmptyDashboard = () => {
  const { t } = useTranslation("dashboard");
  const { realm, setRealm } = useRealm();
  return (
    <PageSection variant="light">
      <EmptyState variant="large">
        <Brand
          src="./icon.svg"
          alt="Keycloak icon"
          className="keycloak__dashboard_icon"
        />
        <Title headingLevel="h4" size="3xl">
          {t("welcome")}
        </Title>
        <Title headingLevel="h4" size="4xl">
          {realm}
        </Title>
        <EmptyStateBody>{t("introduction")}</EmptyStateBody>
        <Button variant="link" onClick={() => setRealm("master")}>
          {t("common:realmInfo")}
        </Button>
      </EmptyState>
    </PageSection>
  );
};

const Dashboard = () => {
  const { t } = useTranslation("dashboard");
  const { realm } = useRealm();
  const serverInfo = useServerInfo();

  const enabledFeatures = _.xor(
    serverInfo.profileInfo?.disabledFeatures,
    serverInfo.profileInfo?.experimentalFeatures,
    serverInfo.profileInfo?.previewFeatures
  );

  const isExperimentalFeature = (feature: string) => {
    return serverInfo.profileInfo?.experimentalFeatures?.includes(feature);
  };

  const isPreviewFeature = (feature: string) => {
    return serverInfo.profileInfo?.previewFeatures?.includes(feature);
  };

  return (
    <>
      <PageSection variant="light">
        <TextContent className="pf-u-mr-sm">
          <Text component="h1">{toUpperCase(realm)} realm</Text>
        </TextContent>
      </PageSection>
      <PageSection>
        <Grid hasGutter>
          <GridItem lg={2} sm={12}>
            <Card className="keycloak__dashboard_card">
              <CardTitle>{t("serverInfo")}</CardTitle>
              <CardBody>
                <DescriptionList>
                  <DescriptionListGroup>
                    <DescriptionListTerm>{t("version")}</DescriptionListTerm>
                    <DescriptionListDescription>
                      {serverInfo.systemInfo?.version}
                    </DescriptionListDescription>
                    <DescriptionListTerm>{t("product")}</DescriptionListTerm>
                    <DescriptionListDescription>
                      {toUpperCase(serverInfo.profileInfo?.name!)}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                </DescriptionList>
              </CardBody>
            </Card>
          </GridItem>
          <GridItem lg={10} sm={12}>
            <Card className="keycloak__dashboard_card">
              <CardTitle>{t("profile")}</CardTitle>
              <CardBody>
                <DescriptionList>
                  <DescriptionListGroup>
                    <DescriptionListTerm>
                      {t("enabledFeatures")}{" "}
                      <HelpItem
                        forID="enabledFeatures"
                        forLabel={t("enabledFeatures")}
                        helpText="dashboard:infoEnabledFeatures"
                      />
                    </DescriptionListTerm>
                    <DescriptionListDescription>
                      <List variant={ListVariant.inline}>
                        {enabledFeatures.map((feature) => (
                          <ListItem key={feature}>
                            {feature}{" "}
                            {isExperimentalFeature(feature) ? (
                              <Label color="orange">{t("experimental")}</Label>
                            ) : (
                              <></>
                            )}
                            {isPreviewFeature(feature) ? (
                              <Label color="blue">{t("preview")}</Label>
                            ) : (
                              <></>
                            )}
                          </ListItem>
                        ))}
                      </List>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                  <DescriptionListGroup>
                    <DescriptionListTerm>
                      {t("disabledFeatures")}{" "}
                      <HelpItem
                        forID="disabledFeatures"
                        forLabel={t("disabledFeatures")}
                        helpText="dashboard:infoDisabledFeatures"
                      />
                    </DescriptionListTerm>
                    <DescriptionListDescription>
                      <List variant={ListVariant.inline}>
                        {serverInfo.profileInfo?.disabledFeatures?.map(
                          (feature) => (
                            <ListItem key={feature}>{feature}</ListItem>
                          )
                        )}
                      </List>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                </DescriptionList>
              </CardBody>
            </Card>
          </GridItem>
        </Grid>
      </PageSection>
    </>
  );
};

export const DashboardSection = () => {
  const { realm } = useRealm();
  const isMasterRealm = realm === "master";
  return (
    <>
      {!isMasterRealm && <EmptyDashboard />}
      {isMasterRealm && <Dashboard />}
    </>
  );
};
