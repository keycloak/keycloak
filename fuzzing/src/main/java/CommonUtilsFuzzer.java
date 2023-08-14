/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.net.InetSocketAddress;
import org.keycloak.common.util.Encode;
import org.keycloak.common.util.EnvUtil;
import org.keycloak.common.util.FindFile;
import org.keycloak.common.util.HtmlUtils;
import org.keycloak.common.util.NetworkUtils;
import org.keycloak.common.util.PathHelper;
import org.keycloak.common.util.StackUtil;

/**
 * This fuzzer targets the methods in different util
 * classes in the common package.
 */
public class CommonUtilsFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Randomly choose which utils method to invoke
      Integer choice = data.consumeInt(1, 21);
      switch (choice) {
        case 1:
          Encode.encodeQueryString(data.consumeRemainingAsString());
          break;
        case 2:
          Encode.encodePath(data.consumeRemainingAsString());
          break;
        case 3:
          Encode.encodePathSegment(data.consumeRemainingAsString());
          break;
        case 4:
          Encode.encodeFragment(data.consumeRemainingAsString());
          break;
        case 5:
          Encode.encodeMatrixParam(data.consumeRemainingAsString());
          break;
        case 6:
          Encode.encodeQueryParam(data.consumeRemainingAsString());
          break;
        case 7:
          Encode.decodePath(data.consumeRemainingAsString());
          break;
        case 8:
          Encode.encodePathAsIs(data.consumeRemainingAsString());
          break;
        case 9:
          Encode.encodePathSaveEncodings(data.consumeRemainingAsString());
          break;
        case 10:
          Encode.encodePathSegmentAsIs(data.consumeRemainingAsString());
          break;
        case 11:
          Encode.encodePathSegmentSaveEncodings(data.consumeRemainingAsString());
          break;
        case 12:
          Encode.encodeQueryParamAsIs(data.consumeRemainingAsString());
          break;
        case 13:
          Encode.encodeQueryParamSaveEncodings(data.consumeRemainingAsString());
          break;
        case 14:
          Encode.encodeFragmentAsIs(data.consumeRemainingAsString());
          break;
        case 15:
          EnvUtil.replace(data.consumeRemainingAsString());
          break;
        case 16:
          FindFile.findFile(data.consumeRemainingAsString());
          break;
        case 17:
          HtmlUtils.escapeAttribute(data.consumeRemainingAsString());
          break;
        case 18:
          Integer port = data.consumeInt(1, 65536);
          NetworkUtils.formatAddress(new InetSocketAddress(data.consumeRemainingAsString(), port));
          break;
        case 19:
          PathHelper.replaceEnclosedCurlyBraces(data.consumeRemainingAsString());
          break;
        case 20:
          PathHelper.recoverEnclosedCurlyBraces(data.consumeRemainingAsString());
          break;
        case 21:
          StackUtil.getShortStackTrace(data.consumeRemainingAsString());
          break;
      }
    } catch (RuntimeException e) {
      // Known exception
    }
  }
}
