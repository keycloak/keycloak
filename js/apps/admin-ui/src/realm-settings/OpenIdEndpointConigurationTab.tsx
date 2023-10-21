import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  InputGroup,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { addTrailingSlash } from "../util";
import { adminClient } from "../admin-client";
import { MinusIcon, PlusIcon } from "@patternfly/react-icons";
import { getAuthorizationHeaders } from "../utils/getAuthorizationHeaders";

type RealmSettingsThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const OpenIdEndpointConigurationTab = ({
  realm,
  save,
}: RealmSettingsThemesTabProps) => {
  const { t } = useTranslation();

  const [newClaim, setNewClaim] = useState<string>("");
  const { control, handleSubmit, setValue } = useForm<RealmRepresentation>();

  const setupForm = () => {
    let attributes = realm.attributes || {};
    
    if (!("claimsSupported" in attributes)) {
      getOpenIdEndpointConfiguration().then((result) => {
        attributes.claimsSupported = result.claims_supported.join(",");
        setValue("attributes", attributes);
      });
    }
    else{
      setValue("attributes", attributes);
    }
    
  };

  useEffect(setupForm, []);

  async function getOpenIdEndpointConfiguration(): Promise<any> {
    const response = await fetch(
      `${addTrailingSlash(adminClient.baseUrl)}/realms/${
        realm.realm
      }/.well-known/openid-configuration`,
      {
        method: "GET",
        headers: getAuthorizationHeaders(await adminClient.getAccessToken()),
      },
    );

    if (!response.ok) {
      throw new Error(
        `Server responded with invalid status: ${response.statusText}`,
      );
    }
    return response.json();
  }

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-u-mt-lg"
        onSubmit={handleSubmit(save)}
      >
        <FormGroup
          label={t("claimsSupported")}
          fieldId="kc-claims-supported"
          labelIcon={
            <HelpItem
              helpText={t("claimsSupportedHelp")}
              fieldLabelId="claimsSupported"
            />
          }
        >
          <Controller
            name="attributes"
            control={control}
            render={({ field }) => {
              const claimsSupported = field.value?.claimsSupported?.trim()
                ? field.value.claimsSupported.split(",")
                : [];
              return (
                <>
                  {claimsSupported.map((claim: string, index: number) => {
                    if (claim) {
                      return (
                        <InputGroup key={index}>
                          <TextInput
                            id={"textInput" + index}
                            value={claim}
                            onChange={(value) => {
                              claimsSupported[index] = value;
                              field.value &&
                                (field.value.claimsSupported =
                                  claimsSupported.join(","));
                              field.onChange(field.value);
                            }}
                            aria-label="input with button"
                          />
                          <Button
                            id={"inputButtonRemove" + index}
                            icon={<MinusIcon />}
                            onClick={() => {
                              claimsSupported.splice(index, 1);
                              field.value &&
                                (field.value.claimsSupported =
                                  claimsSupported.join(","));
                              field.onChange(field.value);
                            }}
                            variant="control"
                          />
                        </InputGroup>
                      );
                    }
                  })}
                  <InputGroup>
                    <TextInput
                      id="textInputAdd"
                      value={newClaim}
                      onChange={(value) => {
                        setNewClaim(value);
                      }}
                      aria-label="input with button"
                    />
                    <Button
                      id={"inputButtonAdd"}
                      icon={<PlusIcon />}
                      onClick={() => {
                        if (newClaim) {
                          claimsSupported.push(newClaim);
                          field.value &&
                            (field.value.claimsSupported =
                              claimsSupported.join(","));
                          setNewClaim("");
                          field.onChange(field.value);
                        }
                      }}
                      variant="control"
                    />
                  </InputGroup>
                </>
              );
            }}
          />
        </FormGroup>

        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="openid-configuration-tab-save"
          >
            {t("save")}
          </Button>
          <Button variant="link" onClick={setupForm}>
            {t("revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
