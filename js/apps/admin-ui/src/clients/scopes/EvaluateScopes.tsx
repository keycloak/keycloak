import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import {
  HelpItem,
  KeycloakDataTable,
  KeycloakSelect,
  SelectVariant,
  useFetch,
  useHelp,
} from "@keycloak/keycloak-ui-shared";
import {
  ClipboardCopy,
  Form,
  FormGroup,
  Grid,
  GridItem,
  PageSection,
  SelectOption,
  Split,
  SplitItem,
  Tab,
  TabContent,
  Tabs,
  TabTitleText,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useEffect, useRef, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { ClientSelect } from "../../components/client/ClientSelect";
import { UserSelect } from "../../components/users/UserSelect";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { prettyPrintJSON } from "../../util";
import { GeneratedCodeTab } from "./GeneratedCodeTab";

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
      ariaLabelKey="effectiveProtocolMappers"
      searchPlaceholderKey="searchForProtocol"
      data-testid="effective-protocol-mappers"
      columns={[
        {
          name: "mapperName",
          displayKey: "name",
        },
        {
          name: "containerName",
          displayKey: "parentClientScope",
        },
        {
          name: "type.category",
          displayKey: "category",
        },
        {
          name: "type.priority",
          displayKey: "priority",
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
      ariaLabelKey="effectiveRoleScopeMappings"
      searchPlaceholderKey="searchForRole"
      data-testid="effective-role-scope-mappings"
      columns={[
        {
          name: "name",
          displayKey: "role",
        },
        {
          name: "containerId",
          displayKey: "origin",
        },
      ]}
    />
  );
};

export const EvaluateScopes = ({ clientId, protocol }: EvaluateScopesProps) => {
  const { adminClient } = useAdminClient();

  const prefix = "openid";
  const { t } = useTranslation();
  const { enabled } = useHelp();
  const { realm } = useRealm();
  const mapperTypes = useServerInfo().protocolMapperTypes![protocol];

  const [selectableScopes, setSelectableScopes] = useState<
    ClientScopeRepresentation[]
  >([]);
  const [isScopeOpen, setIsScopeOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>([prefix]);
  const [activeTab, setActiveTab] = useState(0);

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);
  const [effectiveRoles, setEffectiveRoles] = useState<RoleRepresentation[]>(
    [],
  );
  const [protocolMappers, setProtocolMappers] = useState<
    ProtocolMapperRepresentation[]
  >([]);
  const [accessToken, setAccessToken] = useState("");
  const [userInfo, setUserInfo] = useState("");
  const [idToken, setIdToken] = useState("");

  const tabContent1 = useRef(null);
  const tabContent2 = useRef(null);
  const tabContent3 = useRef(null);
  const tabContent4 = useRef(null);
  const tabContent5 = useRef(null);

  const form = useForm();
  const { watch } = form;
  const selectedAudience: string[] = watch("targetAudience");

  const { hasAccess } = useAccess();
  const hasViewUsers = hasAccess("view-users");

  useFetch(
    () => adminClient.clients.listOptionalClientScopes({ id: clientId }),
    (optionalClientScopes) => setSelectableScopes(optionalClientScopes),
    [],
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
        mapper.type = mapperTypes.find(
          (type) => type.id === mapper.protocolMapper,
        )!;
      });

      setProtocolMappers(mapperList);
      refresh();
    },
    [selected],
  );

  useFetch(
    async () => {
      const scope = selected.join(" ");
      const user = form.getValues("user");
      if (user.length === 0) {
        return [];
      }
      const audience = selectedAudience.join(" ");

      return await Promise.all([
        adminClient.clients.evaluateGenerateAccessToken({
          id: clientId,
          userId: user[0],
          scope,
          audience,
        }),
        adminClient.clients.evaluateGenerateUserInfo({
          id: clientId,
          userId: user[0],
          scope,
        }),
        adminClient.clients.evaluateGenerateIdToken({
          id: clientId,
          userId: user[0],
          scope,
        }),
      ]);
    },
    ([accessToken, userInfo, idToken]) => {
      setAccessToken(prettyPrintJSON(accessToken));
      setUserInfo(prettyPrintJSON(userInfo));
      setIdToken(prettyPrintJSON(idToken));
    },
    [form.getValues("user"), selected, selectedAudience],
  );

  return (
    <>
      <PageSection variant="light">
        {enabled && (
          <TextContent className="keycloak__section_intro__help">
            <Text>
              <QuestionCircleIcon /> {t("evaluateExplain")}
            </Text>
          </TextContent>
        )}
        <Form isHorizontal>
          <FormGroup
            label={t("scopeParameter")}
            fieldId="scopeParameter"
            labelIcon={
              <HelpItem
                helpText={t("scopeParameterHelp")}
                fieldLabelId="scopeParameter"
              />
            }
          >
            <Split hasGutter>
              <SplitItem isFilled>
                <KeycloakSelect
                  toggleId="scopeParameter"
                  variant={SelectVariant.typeaheadMulti}
                  typeAheadAriaLabel={t("scopeParameter")}
                  onToggle={() => setIsScopeOpen(!isScopeOpen)}
                  isOpen={isScopeOpen}
                  selections={selected}
                  onSelect={(value) => {
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
                    <SelectOption key={index} value={option.name}>
                      {option.name}
                    </SelectOption>
                  ))}
                </KeycloakSelect>
              </SplitItem>
              <SplitItem>
                <ClipboardCopy className="keycloak__scopes_evaluate__clipboard-copy">
                  {selected.join(" ")}
                </ClipboardCopy>
              </SplitItem>
            </Split>
          </FormGroup>
          {hasViewUsers && (
            <FormProvider {...form}>
              <UserSelect
                name="user"
                label="users"
                helpText={t("userHelp")}
                defaultValue=""
                variant={SelectVariant.typeahead}
                isRequired
              />
            </FormProvider>
          )}
          <FormProvider {...form}>
            <ClientSelect
              name="targetAudience"
              label={t("targetAudience")}
              helpText={t("targetAudienceHelp")}
              defaultValue={[]}
              variant="typeaheadMulti"
              placeholderText={t("targetAudiencePlaceHolder")}
            />
          </FormProvider>
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
            aria-labelledby={t("generatedAccessToken")}
            eventKey={2}
            id="tab-generated-access-token"
            ref={tabContent3}
            hidden
          >
            <GeneratedCodeTab
              text={accessToken}
              user={form.getValues("user")}
              label="generatedAccessToken"
            />
          </TabContent>
          <TabContent
            aria-labelledby={t("generatedIdToken")}
            eventKey={3}
            id="tab-generated-id-token"
            ref={tabContent4}
            hidden
          >
            <GeneratedCodeTab
              text={idToken}
              user={form.getValues("user")}
              label="generatedIdToken"
            />
          </TabContent>
          <TabContent
            aria-labelledby={t("generatedUserInfo")}
            eventKey={4}
            id="tab-generated-user-info"
            ref={tabContent5}
            hidden
          >
            <GeneratedCodeTab
              text={userInfo}
              user={form.getValues("user")}
              label="generatedUserInfo"
            />
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
              data-testid="effective-protocol-mappers-tab"
              eventKey={0}
              title={
                <TabTitleText>
                  {t("effectiveProtocolMappers")}{" "}
                  <HelpItem
                    fieldLabelId="effectiveProtocolMappers"
                    helpText={t("effectiveProtocolMappersHelp")}
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
              data-testid="effective-role-scope-mappings-tab"
              eventKey={1}
              title={
                <TabTitleText>
                  {t("effectiveRoleScopeMappings")}{" "}
                  <HelpItem
                    fieldLabelId="effectiveRoleScopeMappings"
                    helpText={t("effectiveRoleScopeMappingsHelp")}
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
              data-testid="generated-access-token-tab"
              eventKey={2}
              title={
                <TabTitleText>
                  {t("generatedAccessToken")}{" "}
                  <HelpItem
                    fieldLabelId="generatedAccessToken"
                    helpText={t("generatedAccessTokenHelp")}
                    noVerticalAlign={false}
                    unWrap
                  />
                </TabTitleText>
              }
              tabContentRef={tabContent3}
            />
            <Tab
              id="generatedIdToken"
              aria-controls="generatedIdToken"
              data-testid="generated-id-token-tab"
              eventKey={3}
              title={
                <TabTitleText>
                  {t("generatedIdToken")}{" "}
                  <HelpItem
                    fieldLabelId="generatedIdToken"
                    helpText={t("generatedIdTokenHelp")}
                    noVerticalAlign={false}
                    unWrap
                  />
                </TabTitleText>
              }
              tabContentRef={tabContent4}
            />
            <Tab
              id="generatedUserInfo"
              aria-controls="generatedUserInfo"
              data-testid="generated-user-info-tab"
              eventKey={4}
              title={
                <TabTitleText>
                  {t("generatedUserInfo")}{" "}
                  <HelpItem
                    fieldLabelId="generatedUserInfo"
                    helpText={t("generatedUserInfoHelp")}
                    noVerticalAlign={false}
                    unWrap
                  />
                </TabTitleText>
              }
              tabContentRef={tabContent5}
            />
          </Tabs>
        </GridItem>
      </Grid>
    </>
  );
};
