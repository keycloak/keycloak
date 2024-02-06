import { useEffect, useState } from "react";
import type { OrgRepresentation } from "./routes";
import useOrgFetcher from "./useOrgFetcher";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  ActionGroup,
  Button,
  Text,
  FormGroup,
  FormSelect,
  FormSelectOption,
  Grid,
  GridItem,
  TextVariants,
  Alert,
  AlertGroup,
  AlertVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { adminClient } from "../admin-client";
import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { isNil } from "lodash-es";
import { useNavigate, generatePath } from "react-router-dom";

type OrgIdentityProvidersProps = {
  org: OrgRepresentation;
};

interface AlertInfo {
  title: string;
  variant: AlertVariant;
  key: number;
}

interface IdentityProviderRepresentationP2
  extends IdentityProviderRepresentation {
  config: {
    "home.idp.discovery.org"?: string;
  };
}

export default function OrgIdentityProviders({
  org,
}: OrgIdentityProvidersProps) {
  console.log("[OrgIdentityProviders org]", org);

  const { realm } = useRealm();
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { updateIdentityProvider } = useOrgFetcher(realm);
  const { t } = useTranslation();
  const [idps, setIdps] = useState<IdentityProviderRepresentationP2[]>([]);
  const disabledSelectorText = "please choose";
  const [isUpdatingIdP, setisUpdatingIdP] = useState(false);
  const [selectedIdP, setSelectedIdP] = useState<string>(disabledSelectorText);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [enabledIdP, setEnabledIdP] =
    useState<IdentityProviderRepresentationP2>();
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState<AlertInfo[]>([]);
  const getUniqueId: () => number = () => new Date().getTime();

  // console.log("[adminClient]", adminClient);

  async function getIDPs() {
    const identityProviders = (await adminClient.identityProviders.find({
      realm,
    })) as IdentityProviderRepresentationP2[];
    console.log("[identityProviders]", identityProviders);
    setIdps(identityProviders);

    // at least one IdP?
    // find the enabled IdP applicable to this org
    const enabledIdP = identityProviders.find((idp) => {
      // if the key `home.idp.discovery.org` exists
      // and the key is equal to the org id and idp is enabled
      if (isNil(idp.config["home.idp.discovery.org"])) {
        return false;
      }
      return idp.config["home.idp.discovery.org"] === org.id && idp.enabled;
    });
    if (enabledIdP) {
      setEnabledIdP(enabledIdP);
    }
  }

  useEffect(() => {
    getIDPs();
  }, []);

  const onChange = (value: string) => setSelectedIdP(value);

  const save = async () => {
    setisUpdatingIdP(true);
    const fullSelectedIdp = idps.find((i) => i.internalId === selectedIdP)!;
    try {
      // enabledIdP? Set org to empty
      if (enabledIdP) {
        const respE = await updateIdentityProvider(
          { ...enabledIdP, config: { "home.idp.discovery.org": "" } },
          enabledIdP.alias!,
        );
        if (respE.error) {
          throw new Error("Failed to disable existing IdP.");
        }
      }

      const resp = await updateIdentityProvider(
        {
          ...fullSelectedIdp,
          postBrokerLoginFlowAlias: "post org broker login",
          config: {
            ...fullSelectedIdp.config,
            syncmode: "FORCE",
            hideOnLoginPage: "true",
            "home.idp.discovery.org": org.id,
          },
        },
        fullSelectedIdp.alias!,
      );

      if (resp.error) {
        throw new Error("Failed to update new IdP.");
      }

      setAlerts((prevAlertInfo) => [
        ...prevAlertInfo,
        {
          title: resp.message as string,
          variant: AlertVariant.success,
          key: getUniqueId(),
        },
      ]);
    } catch (e) {
      console.log("Error during update", e);
      setAlerts((prevAlertInfo) => [
        ...prevAlertInfo,
        {
          title: "IdP failed to update for this org. Please try again.",
          variant: AlertVariant.danger,
          key: getUniqueId(),
        },
      ]);
    }
    setisUpdatingIdP(false);
  };

  const options = [
    { value: disabledSelectorText, label: "Select one", disabled: true },
    ...idps
      .filter((idp) => idp.internalId !== enabledIdP?.internalId)
      .filter((idp) =>
        isNil(idp.config["home.idp.discovery.org"])
          ? true
          : idp.config["home.idp.discovery.org"] === org.id,
      )
      .map((idp) => {
        let label = `${idp.displayName} (${idp.alias})`;
        if (!isNil(idp.config["home.idp.discovery.org"])) {
          label = `${label} - ${org.displayName}`;
        }
        return {
          value: idp.internalId,
          label: label,
          disabled: false,
        };
      }),
  ];

  let body = (
    <Text component={TextVariants.h1}>No IdPs assigned to this Org.</Text>
  );

  const buttonsDisabled = selectedIdP === disabledSelectorText;

  if (idps.length > 0) {
    body = (
      <div>
        <AlertGroup
          isLiveRegion
          aria-live="polite"
          aria-relevant="additions text"
          aria-atomic="false"
        >
          {alerts.map(({ title, variant, key }) => (
            <Alert
              variant={variant}
              title={title}
              key={key}
              timeout={8000}
              className="pf-u-mb-lg"
            />
          ))}
        </AlertGroup>
        <Text component={TextVariants.h1}>
          {enabledIdP ? (
            <>
              <strong>Organization Owned IdP</strong>: {enabledIdP.displayName}{" "}
              ({enabledIdP.alias})
              <Button
                variant="link"
                onClick={() =>
                  navigate(
                    generatePath(
                      "/auth/admin/:realm/console/#/self/identity-providers/:providerId/:alias/settings",
                      {
                        realm,
                        providerId: enabledIdP.providerId!,
                        alias: enabledIdP.alias!,
                      },
                    ),
                  )
                }
                disabled={buttonsDisabled}
                isDisabled={buttonsDisabled}
              >
                {t("edit")}
              </Button>
            </>
          ) : (
            <div>No Organization Owned IdP Assigned</div>
          )}
        </Text>

        <Grid hasGutter className="pf-u-mt-xl">
          <GridItem span={8}>
            <FormGroup
              label="Select Identity Provider"
              type="string"
              fieldId="idpSelector"
            >
              <FormSelect
                value={selectedIdP}
                onChange={onChange}
                aria-label="Identity Providers"
                id="idpSelector"
              >
                {options.map((option, index) => (
                  <FormSelectOption
                    isDisabled={option.disabled}
                    key={index}
                    value={option.value}
                    label={option.label}
                  />
                ))}
              </FormSelect>
              <Text component={TextVariants.small}>
                Change the active IdP for this Organization.
              </Text>
            </FormGroup>
            <ActionGroup className="pf-u-mt-xl">
              <Button
                onClick={save}
                disabled={buttonsDisabled}
                isDisabled={buttonsDisabled || isUpdatingIdP}
                isLoading={isUpdatingIdP}
              >
                {t("save")}
              </Button>
              <Button
                variant="link"
                onClick={() => setSelectedIdP(disabledSelectorText)}
                disabled={buttonsDisabled || isUpdatingIdP}
                isDisabled={buttonsDisabled}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </GridItem>
        </Grid>
      </div>
    );
  }

  return <div className="pf-u-p-lg">{body}</div>;
}
