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
 
declare const l18nMsg: {[key: string]: string};

export interface MsgProps {
    readonly msgKey: string | undefined;
    readonly params?: string[];
}
 
export class Msg extends React.Component<MsgProps> {

    public constructor(props: MsgProps) {
        super(props);
    }
    
    public render(): React.ReactNode {
        return (
            <React.Fragment>{Msg.localize(this.props.msgKey, this.props.params)}</React.Fragment>
        );
    }
    
    public static localize(msgKey: string | undefined, params?: string[]): string {
        if (msgKey === undefined) return '';
        
        let message: string = l18nMsg[this.processKey(msgKey)];
        if (message === undefined) message = msgKey;
        
        if ((params !== undefined) && (params.length > 0)) {
            params.forEach((value: string, index: number) => {
                value = this.processParam(value);
                message = message.replace('{{param_'+ index + '}}', value);
            })
        }
        
        return unescape(message);
    }

    // if the message key has Freemarker syntax, remove it
    private static processKey(msgKey: string): string {
        if (!(msgKey.startsWith('${') && msgKey.endsWith('}'))) return msgKey;

        // remove Freemarker syntax
        return msgKey.substring(2, msgKey.length - 1);
    }
    
    // if the param has Freemarker syntax, try to look up its value
    private static processParam(param: string): string {
        if (!(param.startsWith('${') && param.endsWith('}'))) return param;

        // remove Freemarker syntax
        const key: string = param.substring(2, param.length - 1);
        
        let value: string = l18nMsg[key];
        if (value === undefined) return param;
        
        return value;
    }
}
