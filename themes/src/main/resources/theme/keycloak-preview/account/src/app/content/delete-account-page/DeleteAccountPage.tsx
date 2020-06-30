import * as React from 'react';
import { ContentPage } from '../ContentPage';
import { Msg } from '../../widgets/Msg';
import { Stack, Alert, Button } from '@patternfly/react-core';
import { MinusIcon, MinusCircleIcon } from '@patternfly/react-icons'
import { Link } from 'react-router-dom';
import { AppInitiatedActionPage } from '../aia-page/AppInitiatedActionPage';


export class DeleteAccountPage extends React.Component {

    constructor(props:any) {
        super(props);
        console.log("----------hehehe delete accout", props)
      } 

      handleDeleteAccount() {
        console.log("test")
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
               <Link to={{pathname: "/aia", state: {pageDef: {kcAction:"delete_account", prompt: "login", label: "deleteAccount", labelParams: ""}}}} > 
               <Button onClick={this.handleDeleteAccount} variant="danger" style={{display:'block', width:'30%', margin:'auto'}}>Delete</Button>
               </Link>
            </Stack>
          </ContentPage>
        );
      }
}