/**
 *
 */
package org.keycloak.util;

import java.util.regex.Pattern;


/**
 * A utility class for handling URI template parameters. As the Java
 * regulare expressions package does not handle named groups, this
 * class attempts to simulate that functionality by using groups.
 *
 * @author Ryan J. McDonough
 * @author Bill Burke
 * @since 1.0
 *        Nov 8, 2006
 */
public class PathHelper
{
   public static final String URI_PARAM_NAME_REGEX = "\\w[\\w\\.-]*";
   public static final String URI_PARAM_REGEX_REGEX = "[^{}][^{}]*";
   public static final String URI_PARAM_REGEX = "\\{\\s*(" + URI_PARAM_NAME_REGEX + ")\\s*(:\\s*(" + URI_PARAM_REGEX_REGEX + "))?\\}";
   public static final Pattern URI_PARAM_PATTERN = Pattern.compile(URI_PARAM_REGEX);

   /**
    * A regex pattern that searches for a URI template parameter in the form of {*}
    */
   public static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("(\\{([^}]+)\\})");

   public static final char openCurlyReplacement = 6;
   public static final char closeCurlyReplacement = 7;

   public static String replaceEnclosedCurlyBraces(String str)
   {
      char[] chars = str.toCharArray();
      int open = 0;
      for (int i = 0; i < chars.length; i++)
      {
         if (chars[i] == '{')
         {
            if (open != 0) chars[i] = openCurlyReplacement;
            open++;
         }
         else if (chars[i] == '}')
         {
            open--;
            if (open != 0)
            {
               chars[i] = closeCurlyReplacement;
            }
         }
      }
      return new String(chars);
   }

   public static String recoverEnclosedCurlyBraces(String str)
   {
      return str.replace(openCurlyReplacement, '{').replace(closeCurlyReplacement, '}');
   }

}