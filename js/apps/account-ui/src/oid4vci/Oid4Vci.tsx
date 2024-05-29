import {
    Select,
    SelectList,
    SelectOption,
    PageSectionVariants,
    PageSection,
    ActionList,
    ActionListItem,
    List,
    ListItem,
    MenuToggleElement,
    MenuToggle
} from '@patternfly/react-core';
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { usePromise } from "../utils/usePromise";
import { Page } from "../components/page/Page";
import { parseResponse } from "../api/parse-response";
import { token } from '../api/request';

type CredentialsIssuer = {
  credential_issuer: string;
  credential_endpoint: string;
  authorization_servers: string[]; 
  credential_configurations_supported: Record<string, SupportedCredentialConfiguration>
}

type SupportedCredentialConfiguration = {
  id: string,
  format: string,
  scope: string
}

type VCState = {
  dropdownItems: string[],
  selectOptions: Record<string, SupportedCredentialConfiguration>,
  credentialIssuer?: CredentialsIssuer,
  issuerDid: string,
  qrCode: string,
  isOpen: boolean,
  offerQRVisible: boolean
}

export const Oid4Vci = () => {
    const { addAlert, addError } = useAlerts();
    const context = useEnvironment();
   
    const { t } = useTranslation();
    const initialState: VCState = {
      dropdownItems: [],
      selectOptions: new Map<string, SupportedCredentialConfiguration>(),
      issuerDid: "",
      qrCode: "",
      isOpen: false,
      offerQRVisible: false
    }
    const initialSelected = t('verifiableCredentialsSelectionDefault') 

    const [selected, setSelected] = useState<string>(initialSelected);
    const [vcState, setState] = useState<VCState>(initialState);
    const url:string = context.environment.authUrl
    const realm = context.environment.realm
    const wellKnownIssuer = url + "/realms/" + realm + "/.well-known/openid-credential-issuer"

    usePromise(
      (signal) =>
          getIssuer(wellKnownIssuer),
      (issuer) => {
        const ccsMap = new Map(Object.entries(issuer.credential_configurations_supported))
        const ccsKeyArray = Array.from(ccsMap.keys());
        setState({...vcState , credentialIssuer: issuer, dropdownItems: ccsKeyArray, selectOptions: ccsMap});
    
    });

    useEffect(() => {
      if(initialSelected !== selected){
        requestVCOffer();
      }
    }, [selected]);

    async function fetchWithToken(url: string) {
      var options = {  
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${await token(context.keycloak)?.()}`
        }
      }
      return fetch(url, options)
    }
    
    // retrieve the issuer information for the current session
    const getIssuer = async (wellKnownIssuer: string): Promise<CredentialsIssuer> => {
      return fetchWithToken(wellKnownIssuer) 
        .then(response => parseResponse<CredentialsIssuer>(response)) 
    }

    // request a verifiable credentials offer, already qr-encoded
    const requestVCOffer = () => {
      let supportedCredential = vcState.selectOptions.get(selected)
      if (supportedCredential === undefined) {
        addAlert(t('verifiableCredentialsConfigAlert'))
        return
      }

      let credentialIssuer = vcState.credentialIssuer

      if (credentialIssuer == null) {
        addAlert(t('verifiableCredentialsIssuerAlert'))
        return
      } else {
        const requestUrl = credentialIssuer.credential_issuer + "/protocol/oid4vc/credential-offer-uri?credential_configuration_id=" + supportedCredential.id+ "&type=qr-code&width=500&height=500"
    
        return fetchWithToken(requestUrl) 
        .then(response => handleOfferResponse(response))
      }
    }

  
    const handleOfferResponse = (response: Response) => {
      response.blob()
        .then((blob) => {
          if (response.status !== 200) {
            addError(t('verifiableCredentialsOfferAlert'));
          } else {
            var reader = new FileReader();
            reader.readAsDataURL(blob)
            reader.onloadend = function() {
              let result = reader.result
              if (typeof result === "string") {
                setState({ ...vcState,
                  qrCode: result,
                  offerQRVisible: true,
                  isOpen: false});
              }    
            }
          }
        })    
    }

    const setOpen = (isOpen: boolean) => {
      setState({...vcState,
        isOpen
      });
    }
  
    const onToggleClick = () => {
      setOpen(!vcState.isOpen);
    };

    const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          onClick={onToggleClick}
          isExpanded={vcState.isOpen}
          data-testid="menu-toggle"
        >
          {selected}
        </MenuToggle>
      );

    return (
      <Page title={t('verifiableCredentialsTitle')} description={t('verifiableCredentialsDescription')}>
        <PageSection isFilled variant={PageSectionVariants.light}>     
          <List isPlain>  
            <ListItem>
              <Select
                data-testid="credential-select"
                onOpenChange={(isOpen) => setOpen(isOpen)}
                onSelect={(_event, val) => setSelected(val as string)}
                isOpen={vcState.isOpen}
                selected={selected}
                toggle={toggle}
                shouldFocusToggleOnSelect={true}
              >
                <SelectList>
                  {vcState.dropdownItems.map((option, index) => (
                    <SelectOption 
                      value={option}
                      data-testid='select-${option}'
                    >
                    {option}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
              </ListItem>  
              <ListItem>
                  <ActionList>
                  { vcState.offerQRVisible &&
                    <ActionListItem>
                        <img width='500' height='500' src={`${vcState.qrCode}`} data-testid="qr-code"/>
                    </ActionListItem>
                  }
                  </ActionList>
            </ListItem>
          </List>       
        </PageSection>   
      </Page>
    );
};


export default Oid4Vci;