import * as React from 'react';
import { ContentPage } from '../ContentPage';
import { Msg } from '../../widgets/Msg';
import { Stack, Alert, Button } from '@patternfly/react-core';
import { MinusIcon } from '@patternfly/react-icons'
import { AIACommand } from '../../util/AIACommand';
import { KeycloakContext } from '../../keycloak-service/KeycloakContext';
import { KeycloakService } from '../../keycloak-service/keycloak.service';


export class DeleteAccountPage extends React.Component {

    constructor(props:any) {
        super(props);
      } 

      handleDeleteAccount(keycloak: KeycloakService) {
        new AIACommand(keycloak, "delete_account", 'login').execute();
      }

      render() {
        return (
          <ContentPage title="deleteAccount"
                introMessage="deleteAccountSummary">
             <Stack gutter="md">
             <Alert
          variant="warning"
          title={Msg.localize('irreversibleAction')}></Alert>
                    <p><Msg msgKey="deletingImplies" /></p>
                    <ul>
                      <li><MinusIcon></MinusIcon> <Msg msgKey="loggingOutImmediately" /></li>
                      <li><MinusIcon></MinusIcon> <Msg msgKey="errasingData" /></li>
                      <li><MinusIcon></MinusIcon> <Msg msgKey="accountUnusable" /></li>
                    </ul>

               <p> <Msg msgKey="initialDeletionInstruction" /></p>
               <KeycloakContext.Consumer>
                { (keycloak: KeycloakService) => (
                  <Button onClick={() => this.handleDeleteAccount(keycloak)} variant="danger" style={{display:'block', width:'30%', margin:'auto'}}>Delete</Button>
                )}
                </KeycloakContext.Consumer>
               
            </Stack>
          </ContentPage>
        );
      }
}