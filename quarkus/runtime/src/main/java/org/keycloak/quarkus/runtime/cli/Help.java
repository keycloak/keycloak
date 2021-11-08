/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.cli;

import static picocli.CommandLine.Help.Column.Overflow.SPAN;
import static picocli.CommandLine.Help.Column.Overflow.WRAP;

import org.keycloak.utils.StringUtil;

import picocli.CommandLine;

public class Help extends CommandLine.Help {

    private static final int HELP_WIDTH = 100;

    public Help(CommandLine.Model.CommandSpec commandSpec, ColorScheme colorScheme) {
        super(commandSpec, colorScheme);
    }

    @Override
    public Layout createDefaultLayout() {
        return new Layout(colorScheme(), createTextTable(), createDefaultOptionRenderer(), createDefaultParameterRenderer());
    }

    private TextTable createTextTable() {
        int longOptionsColumnWidth = commandSpec().commandLine().getUsageHelpLongOptionsMaxWidth();
        int descriptionWidth = HELP_WIDTH - longOptionsColumnWidth;

        // save space by using only two columns with better control over how option names and description are rendered
        // for now, no support for required options
        // picocli has a limit of 2 chars for shortnames, we do not
        TextTable textTable = TextTable.forColumns(colorScheme(),
                new Column(longOptionsColumnWidth, 0, SPAN),  // " -cf, --config-file"
                new Column(descriptionWidth, 1, WRAP));

        textTable.setAdjustLineBreaksForWideCJKCharacters(commandSpec().usageMessage().adjustLineBreaksForWideCJKCharacters());

        return textTable;
    }

    @Override
    public IOptionRenderer createDefaultOptionRenderer() {
        return new OptionRenderer();
    }

    @Override
    public String createHeading(String text, Object... params) {
        if (StringUtil.isBlank(text)) {
            return super.createHeading(text, params);
        }
        return super.createHeading("%n@|bold " + text + "|@%n%n", params);
    }

    @Override
    public IParameterRenderer createDefaultParameterRenderer() {
        return new IParameterRenderer() {
            @Override
            public Ansi.Text[][] render(CommandLine.Model.PositionalParamSpec param,
                    IParamLabelRenderer parameterLabelRenderer, ColorScheme scheme) {
                // we do our own formatting of parameters and labels when rendering optionsq
                return new Ansi.Text[0][];
            }
        };
    }
}
