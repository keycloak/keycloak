/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';

import { TranslateModule } from '@ngx-translate/core';

import {WidgetsModule} from '../widgets/widgets.module';

import { ApplicationsPageComponent } from './applications-page.component';
import { ApplicationsRoutingModule } from './applications-routing.module';
import { LargeAppCardComponent } from './large-app-card.component';
import { SmallAppCardComponent } from './small-app-card.component';
import { RowAppCardComponent } from './row-app-card.component';

@NgModule({
  imports:      [ CommonModule, 
                  TranslateModule, 
                  ApplicationsRoutingModule,
                  WidgetsModule ],
  declarations: [ ApplicationsPageComponent,
                  LargeAppCardComponent,
                  SmallAppCardComponent,
                  RowAppCardComponent ],
  providers:    [ ]
})
export class ApplicationsModule {}



