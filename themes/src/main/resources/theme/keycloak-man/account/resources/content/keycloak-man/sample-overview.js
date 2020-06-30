/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { Msg } from "../../widgets/Msg.js";

// No JSX - no compilation/transpilation needed
export class SampleOverview extends React.Component {
    render() {
        const e = React.createElement;
        return e('div', {class: 'pf-c-card'}, [
            e('center', null, e("img", {class: 'pf-c-brand', src: resourceUrl + '/public/keycloak-man-95x95.jpg', alt: 'Keycloak Man Logo'})),
            e('div', {class: 'pf-c-card__body'}, [
                e('p', null, `You can create new pages like this using a React component.  The component is declared in content.json.`),
                e('p', null, `This page only provides an overview of the files and directories.  See Keycloak documentation for more details.`)
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/theme.properties: '),
                e('span', null, 'Defines this theme. Open file for more documentation.')
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/messages/messages_en.properties: '),
                e(Msg, {msgKey: 'youCanLocalize'})
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/resources/content.json: '),
                e('span', null, `Defines pages and navigation for the welcome screen and the main application.
                                 Each navigation item maps to a React component.`)
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/resources/content/keycloak-man/who-is-keycloak-man.js: '),
                e('span', null, `This page demonstrates calling the Keycloak Account REST API.`)
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/resources/content/keycloak-man/sample-overview.js: '),
                e('span', null, `The javascript for this page. It is a 'React without JSX' page.
                                 Though this page only demonstrates using PatternFly CSS classes,
                                 you can also use PatternFly React components if you wish.`)
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/resources/css/styles.css: '),
                e('span', null, `You are free to use any css, but this particular file provides examples of theming using
                                 PatternFly's powerful CSS variables.  This technique is recommended in order to maintain
                                 consistency as long as you are using PatternFly components.`)
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('strong', null, '/src: '),
                e('span', null, `This directory provides a sample npm project that allows you to build pages using React, JSX, and TypeScript.
                                 To get the sample page running:`),
                e('div', {class: 'pf-c-content'}, [
                    e('ol', null, [
                        e('li', null, 'npm install'),
                        e('li', null, 'npm run build'),
                        e('li', null, `Edit content.json and find the section for 'keycloak-man-likes-jsx'.  Set the hidden flag to false.`),
                    ])
                ])
            ]),
        ]);
    }
};