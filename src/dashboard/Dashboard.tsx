import {
  Brand,
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
import { Trans, useTranslation } from "react-i18next";
import { xor } from "lodash";

import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";

import "./dashboard.css";
import { toUpperCase } from "../util";
import { HelpItem } from "../components/help-enabler/HelpItem";
import environment from "../environment";

const EmptyDashboard = () => {
  const { t } = useTranslation("dashboard");
  const { realm } = useRealm();
  return (
    <PageSection variant="light">
      <EmptyState variant="large">
        <Brand
          src={environment.resourceUrl + "/icon.svg"}
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
      </EmptyState>
    </PageSection>
  );
};

const Dashboard = () => {
  const { t } = useTranslation("dashboard");
  const { realm } = useRealm();
  const serverInfo = useServerInfo();

  const enabledFeatures = xor(
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
          <Text component="h1">
            {t("realmName", { name: toUpperCase(realm) })}
          </Text>
          <Text>
            <Trans t={t} i18nKey="adminUiVersion">
              <strong>Admin UI version</strong>
              {{ version: environment.commitHash }}
            </Trans>
          </Text>
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
                        fieldLabelId="dashboard:enabledFeatures"
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
                            ) : null}
                            {isPreviewFeature(feature) ? (
                              <Label color="blue">{t("preview")}</Label>
                            ) : null}
                          </ListItem>
                        ))}
                      </List>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                  <DescriptionListGroup>
                    <DescriptionListTerm>
                      {t("disabledFeatures")}{" "}
                      <HelpItem
                        fieldLabelId="dashboard:disabledFeatures"
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

export default function DashboardSection() {
  const { realm } = useRealm();
  const isMasterRealm = realm === "master";
  return (
    <>
      {!isMasterRealm && <EmptyDashboard />}
      {isMasterRealm && <Dashboard />}
    </>
  );
}
