/* 
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
 
declare const l18n_msg: {[key:string]: string};

export interface MsgProps {
    readonly msgKey:string;
    readonly params?:Array<string>;
}
 
export class Msg extends React.Component<MsgProps> {

    constructor(props: MsgProps) {
        super(props);
    }
    
    render() {
        let message:string = l18n_msg[this.props.msgKey];
        if (message === undefined) message = this.props.msgKey;
        
        if (this.props.params !== undefined) {
            this.props.params.forEach((value: string, index: number) => {
                message = message.replace('{{param_'+ index + '}}', value);
            })
        }
        
        return (
            <span>{message}</span>
        );
    }
}
