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

import static org.keycloak.quarkus.runtime.cli.Picocli.NO_PARAM_LABEL;
import static picocli.CommandLine.Help.Ansi.OFF;

import org.keycloak.utils.StringUtil;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi.Text;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Help.IParamLabelRenderer;
import picocli.CommandLine.Model.OptionSpec;

public class OptionRenderer implements CommandLine.Help.IOptionRenderer {

    private static final String OPTION_NAME_SEPARATOR = ", ";
    private static final Text EMPTY_TEXT = OFF.text("");

    @Override
    public Text[][] render(OptionSpec option, IParamLabelRenderer paramLabelRenderer, ColorScheme scheme) {
        String[] names = option.names();

        if (names.length > 2) {
            throw new CommandLine.PicocliException("Options should have 2 names at most.");
        }

        Text shortName = names.length > 1 ? scheme.optionText(names[0]) : EMPTY_TEXT;
        Text longName = createLongName(option, scheme);
        Text[][] result = new Text[1][];
        String[] descriptions = option.description();

        // for better formatting, only a single line is expected in the description
        // formatting is done by customizations to the text table
        if (descriptions.length > 1) {
            throw new CommandLine.PicocliException("Option[" + option + "] description should have a single line.");
        }

        Text description = formatDescription(descriptions, option, scheme);

        if (EMPTY_TEXT.equals(shortName)) {
            result[0] = new Text[] { longName, description };
        } else {
            result[0] = new Text[] { shortName.concat(OPTION_NAME_SEPARATOR).concat(longName), description };
        }

        return result;
    }

    private Text formatDescription(String[] descriptions, OptionSpec option, ColorScheme scheme) {
        String description = descriptions[0];
        String defaultValue = option.defaultValue();

        if (defaultValue != null) {
            description = description + " Default: " + defaultValue + ".";
        }

        return scheme.text(description);
    }

    private Text createLongName(OptionSpec option, ColorScheme scheme) {
        Text name = scheme.optionText(option.longestName());
        String paramLabel = formatParamLabel(option);

        if (StringUtil.isNotBlank(paramLabel) && !NO_PARAM_LABEL.equals(paramLabel) && !option.usageHelp() && !option.versionHelp()) {
            name = name.concat(" ").concat(paramLabel);
        }

        return name;
    }

    private String formatParamLabel(OptionSpec option) {
        String label = option.paramLabel();

        if (label.startsWith("<") || NO_PARAM_LABEL.equals(label)) {
            return label;
        }

        return "<" + label + ">";
    }
}
