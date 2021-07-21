import {
  ClipboardCopy,
  EmptyState,
  EmptyStateBody,
  Form,
  FormGroup,
  Grid,
  GridItem,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
  Tab,
  TabContent,
  Tabs,
  TabTitleText,
  Text,
  TextArea,
  TextContent,
  Title,
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import type ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import type ProtocolMapperRepresentation from "keycloak-admin/lib/defs/protocolMapperRepresentation";
import type RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import type { ProtocolMapperTypeRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";
import type UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import React, { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHelp } from "../../components/help-enabler/HelpHeader";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import "./evaluate.css";

export type EvaluateScopesProps = {
  clientId: string;
  protocol: string;
};

const ProtocolMappers = ({
  protocolMappers,
}: {
  protocolMappers: ProtocolMapperRepresentation[];
}) => {
  const [key, setKey] = useState(0);
  useEffect(() => {
    setKey(key + 1);
  }, [protocolMappers]);
  return (
    <KeycloakDataTable
      key={key}
      loader={() => Promise.resolve(protocolMappers)}
      ariaLabelKey="clients:effectiveProtocolMappers"
      searchPlaceholderKey="clients:searchForProtocol"
      columns={[
        {
          name: "mapperName",
          displayKey: "common:name",
        },
        {
          name: "containerName",
          displayKey: "clients:parentClientScope",
        },
        {
          name: "type.category",
          displayKey: "common:category",
        },
        {
          name: "type.priority",
          displayKey: "common:priority",
        },
      ]}
    />
  );
};

const EffectiveRoles = ({
  effectiveRoles,
}: {
  effectiveRoles: RoleRepresentation[];
}) => {
  const [key, setKey] = useState(0);
  useEffect(() => {
    setKey(key + 1);
  }, [effectiveRoles]);

  return (
    <KeycloakDataTable
      key={key}
      loader={() => Promise.resolve(effectiveRoles)}
      ariaLabelKey="client:effectiveRoleScopeMappings"
      searchPlaceholderKey="clients:searchForRole"
      columns={[
        {
          name: "name",
          displayKey: "clients:role",
        },
        {
          name: "containerId",
          displayKey: "clients:origin",
        },
      ]}
    />
  );
};

export const EvaluateScopes = ({ clientId, protocol }: EvaluateScopesProps) => {
  const prefix = "openid";
  const { t } = useTranslation("clients");
  const { enabled } = useHelp();
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const mapperTypes = useServerInfo().protocolMapperTypes![protocol];

  const [selectableScopes, setSelectableScopes] = useState<
    ClientScopeRepresentation[]
  >([]);
  const [isScopeOpen, setIsScopeOpen] = useState(false);
  const [isUserOpen, setIsUserOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>([prefix]);
  const [activeTab, setActiveTab] = useState(0);

  const [userItems, setUserItems] = useState<JSX.Element[]>([]);
  const [userSearch, setUserSearch] = useState("");
  const [user, setUser] = useState<UserRepresentation>();

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);
  const [effectiveRoles, setEffectiveRoles] = useState<RoleRepresentation[]>(
    []
  );
  const [protocolMappers, setProtocolMappers] = useState<
    ProtocolMapperRepresentation[]
  >([]);
  const [accessToken, setAccessToken] = useState("");

  const tabContent1 = useRef(null);
  const tabContent2 = useRef(null);
  const tabContent3 = useRef(null);

  useFetch(
    () => adminClient.clients.listOptionalClientScopes({ id: clientId }),
    (optionalClientScopes) => setSelectableScopes(optionalClientScopes),
    []
  );

  const toString = (user: UserRepresentation) => {
    return (
      t("common:fullName", {
        givenName: user.firstName,
        familyName: user.lastName,
      }).trim() ||
      user.username ||
      ""
    );
  };

  useFetch(
    () => {
      if (userSearch.length > 2) {
        return adminClient.users.find({ search: userSearch });
      } else {
        return Promise.resolve<UserRepresentation[]>([]);
      }
    },
    (users) =>
      setUserItems(
        users
          .map((user) => {
            user.toString = function () {
              return toString(this);
            };
            return user;
          })
          .map((user) => <SelectOption key={user.id} value={user} />)
      ),
    [userSearch]
  );

  useFetch(
    async () => {
      const scope = selected.join(" ");
      const effectiveRoles = await adminClient.clients.evaluatePermission({
        id: clientId,
        roleContainer: realm,
        scope,
        type: "granted",
      });

      const mapperList = (await adminClient.clients.evaluateListProtocolMapper({
        id: clientId,
        scope,
      })) as ({
        type: ProtocolMapperTypeRepresentation;
      } & ProtocolMapperRepresentation)[];

      return {
        mapperList,
        effectiveRoles,
      };
    },
    ({ mapperList, effectiveRoles }) => {
      setEffectiveRoles(effectiveRoles);
      mapperList.map((mapper) => {
        mapper.type = mapperTypes.filter(
          (type) => type.id === mapper.protocolMapper
        )[0];
      });

      setProtocolMappers(mapperList);
      refresh();
    },
    [selected]
  );

  useFetch(
    () => {
      const scope = selected.join(" ");
      if (user) {
        return adminClient.clients.evaluateGenerateAccessToken({
          id: clientId,
          userId: user.id!,
          scope,
        });
      } else {
        return Promise.resolve({});
      }
    },
    (accessToken) => {
      setAccessToken(JSON.stringify(accessToken, undefined, 3));
    },
    [user, selected]
  );

  return (
    <>
      <PageSection variant="light">
        {enabled && (
          <TextContent className="keycloak__scopes_evaluate__intro">
            <Text>
              <QuestionCircleIcon /> {t("clients-help:evaluateExplain")}
            </Text>
          </TextContent>
        )}
        <Form isHorizontal>
          <FormGroup
            label={t("scopeParameter")}
            fieldId="scopeParameter"
            labelIcon={
              <HelpItem
                helpText="clients-help:scopeParameter"
                forLabel={t("scopeParameter")}
                forID="scopeParameter"
              />
            }
          >
            <Split hasGutter>
              <SplitItem isFilled>
                <Select
                  toggleId="scopeParameter"
                  variant={SelectVariant.typeaheadMulti}
                  typeAheadAriaLabel={t("scopeParameter")}
                  onToggle={() => setIsScopeOpen(!isScopeOpen)}
                  isOpen={isScopeOpen}
                  selections={selected}
                  onSelect={(_, value) => {
                    const option = value as string;
                    if (selected.includes(option)) {
                      if (option !== prefix) {
                        setSelected(selected.filter((item) => item !== option));
                      }
                    } else {
                      setSelected([...selected, option]);
                    }
                  }}
                  aria-labelledby={t("scopeParameter")}
                  placeholderText={t("scopeParameterPlaceholder")}
                >
                  {selectableScopes.map((option, index) => (
                    <SelectOption key={index} value={option.name} />
                  ))}
                </Select>
              </SplitItem>
              <SplitItem>
                <ClipboardCopy className="keycloak__scopes_evaluate__clipboard-copy">
                  {selected.join(" ")}
                </ClipboardCopy>
              </SplitItem>
            </Split>
          </FormGroup>
          <FormGroup
            label={t("user")}
            fieldId="user"
            labelIcon={
              <HelpItem
                helpText="clients-help:user"
                forLabel={t("user")}
                forID="user"
              />
            }
          >
            <Select
              toggleId="user"
              variant={SelectVariant.typeahead}
              typeAheadAriaLabel={t("user")}
              onToggle={() => setIsUserOpen(!isUserOpen)}
              onFilter={(e) => {
                const value = e?.target.value || "";
                setUserSearch(value);
                return userItems;
              }}
              onClear={() => {
                setUser(undefined);
                setUserSearch("");
              }}
              selections={[user]}
              onSelect={(_, value) => {
                setUser(value as UserRepresentation);
                setUserSearch("");
                setIsUserOpen(false);
              }}
              isOpen={isUserOpen}
            />
          </FormGroup>
        </Form>
      </PageSection>

      <Grid hasGutter className="keycloak__scopes_evaluate__tabs">
        <GridItem span={8}>
          <TabContent
            aria-labelledby="pf-tab-0-effectiveProtocolMappers"
            eventKey={0}
            id="effectiveProtocolMappers"
            ref={tabContent1}
          >
            <ProtocolMappers protocolMappers={protocolMappers} />
          </TabContent>
          <TabContent
            aria-labelledby="pf-tab-0-effectiveRoleScopeMappings"
            eventKey={1}
            id="effectiveRoleScopeMappings"
            ref={tabContent2}
            hidden
          >
            <EffectiveRoles effectiveRoles={effectiveRoles} />
          </TabContent>
          <TabContent
            aria-labelledby="pf-tab-0-generatedAccessToken"
            eventKey={2}
            id="generatedAccessToken"
            ref={tabContent3}
            hidden
          >
            {user && (
              <TextArea rows={20} id="accessToken" value={accessToken} />
            )}
            {!user && (
              <EmptyState variant="large">
                <Title headingLevel="h4" size="lg">
                  {t("noGeneratedAccessToken")}
                </Title>
                <EmptyStateBody>
                  {t("generatedAccessTokenIsDisabled")}
                </EmptyStateBody>
              </EmptyState>
            )}
          </TabContent>
        </GridItem>
        <GridItem span={4}>
          <Tabs
            id="tabs"
            key={key}
            isVertical
            activeKey={activeTab}
            onSelect={(_, key) => setActiveTab(key as number)}
          >
            <Tab
              id="effectiveProtocolMappers"
              aria-controls="effectiveProtocolMappers"
              eventKey={0}
              title={
                <TabTitleText>
                  {t("effectiveProtocolMappers")}{" "}
                  <HelpItem
                    forID="effectiveProtocolMappers"
                    forLabel={t("effectiveProtocolMappers")}
                    helpText="clients-help:effectiveProtocolMappers"
                    noVerticalAlign={false}
                    unWrap
                  />
                </TabTitleText>
              }
              tabContentRef={tabContent1}
            />
            <Tab
              id="effectiveRoleScopeMappings"
              aria-controls="effectiveRoleScopeMappings"
              eventKey={1}
              title={
                <TabTitleText>
                  {t("effectiveRoleScopeMappings")}{" "}
                  <HelpItem
                    forID="effectiveRoleScopeMappings"
                    forLabel={t("effectiveRoleScopeMappings")}
                    helpText="clients-help:effectiveRoleScopeMappings"
                    noVerticalAlign={false}
                    unWrap
                  />
                </TabTitleText>
              }
              tabContentRef={tabContent2}
            ></Tab>
            <Tab
              id="generatedAccessToken"
              aria-controls="generatedAccessToken"
              eventKey={2}
              title={
                <TabTitleText>
                  {t("generatedAccessToken")}{" "}
                  <HelpItem
                    forID="generatedAccessToken"
                    forLabel={t("generatedAccessToken")}
                    helpText="clients-help:generatedAccessToken"
                    noVerticalAlign={false}
                    unWrap
                  />
                </TabTitleText>
              }
              tabContentRef={tabContent3}
            />
          </Tabs>
        </GridItem>
      </Grid>
    </>
  );
};
