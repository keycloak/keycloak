package org.keycloak.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Encode
{
   private static final String UTF_8 = "UTF-8";

   private static final Pattern PARAM_REPLACEMENT = Pattern.compile("_resteasy_uri_parameter");

   private static final String[] pathEncoding = new String[128];
   private static final String[] pathSegmentEncoding = new String[128];
   private static final String[] matrixParameterEncoding = new String[128];
   private static final String[] queryNameValueEncoding = new String[128];
   private static final String[] queryStringEncoding = new String[128];

   static
   {
      /*
       * Encode via <a href="http://ietf.org/rfc/rfc3986.txt">RFC 3986</a>.  PCHAR is allowed allong with '/'
       *
       * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
       * sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
                     / "*" / "+" / "," / ";" / "="
       * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
       *
       */
      for (int i = 0; i < 128; i++)
      {
         if (i >= 'a' && i <= 'z') continue;
         if (i >= 'A' && i <= 'Z') continue;
         if (i >= '0' && i <= '9') continue;
         switch ((char) i)
         {
            case '-':
            case '.':
            case '_':
            case '~':
            case '!':
            case '$':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case '/':
            case ';':
            case '=':
            case ':':
            case '@':
               continue;
         }
         StringBuffer sb = new StringBuffer();
         sb.append((char) i);
         pathEncoding[i] = URLEncoder.encode(sb.toString());
      }
      pathEncoding[' '] = "%20";
      System.arraycopy(pathEncoding, 0, matrixParameterEncoding, 0, pathEncoding.length);
      matrixParameterEncoding[';'] = "%3B";
      matrixParameterEncoding['='] = "%3D";
      matrixParameterEncoding['/'] = "%2F"; // RESTEASY-729
      System.arraycopy(pathEncoding, 0, pathSegmentEncoding, 0, pathEncoding.length);
      pathSegmentEncoding['/'] = "%2F";
      /*
       * Encode via <a href="http://ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
       *
       * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
       * space encoded as '+'
       *
       */
      for (int i = 0; i < 128; i++)
      {
         if (i >= 'a' && i <= 'z') continue;
         if (i >= 'A' && i <= 'Z') continue;
         if (i >= '0' && i <= '9') continue;
         switch ((char) i)
         {
            case '-':
            case '.':
            case '_':
            case '~':
            case '?':
               continue;
            case ' ':
               queryNameValueEncoding[i] = "+";
               continue;
         }
         StringBuffer sb = new StringBuffer();
         sb.append((char) i);
         queryNameValueEncoding[i] = URLEncoder.encode(sb.toString());
      }

      /*
       * query       = *( pchar / "/" / "?" )

       */
      for (int i = 0; i < 128; i++)
      {
         if (i >= 'a' && i <= 'z') continue;
         if (i >= 'A' && i <= 'Z') continue;
         if (i >= '0' && i <= '9') continue;
         switch ((char) i)
         {
            case '-':
            case '.':
            case '_':
            case '~':
            case '!':
            case '$':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case ';':
            case '=':
            case ':':
            case '@':
            case '?':
            case '/':
               continue;
            case ' ':
               queryStringEncoding[i] = "%20";
               continue;
         }
         StringBuffer sb = new StringBuffer();
         sb.append((char) i);
         queryStringEncoding[i] = URLEncoder.encode(sb.toString());
      }
   }

   /**
    * Keep encoded values "%..." and template parameters intact.
    */
   public static String encodeQueryString(String value)
   {
      return encodeValue(value, queryStringEncoding);
   }

   /**
    * Keep encoded values "%...", matrix parameters, template parameters, and '/' characters intact.
    */
   public static String encodePath(String value)
   {
      return encodeValue(value, pathEncoding);
   }

   /**
    * Keep encoded values "%...", matrix parameters and template parameters intact.
    */
   public static String encodePathSegment(String value)
   {
      return encodeValue(value, pathSegmentEncoding);
   }

   /**
    * Keep encoded values "%..." and template parameters intact.
    */
   public static String encodeFragment(String value)
   {
      return encodeValue(value, queryNameValueEncoding);
   }

   /**
    * Keep encoded values "%..." and template parameters intact.
    */
   public static String encodeMatrixParam(String value)
   {
      return encodeValue(value, matrixParameterEncoding);
   }

   /**
    * Keep encoded values "%..." and template parameters intact.
    */
   public static String encodeQueryParam(String value)
   {
      return encodeValue(value, queryNameValueEncoding);
   }

   //private static final Pattern nonCodes = Pattern.compile("%([^a-fA-F0-9]|$)");
   private static final Pattern nonCodes = Pattern.compile("%([^a-fA-F0-9]|[a-fA-F0-9]$|$|[a-fA-F0-9][^a-fA-F0-9])");
   private static final Pattern encodedChars = Pattern.compile("%([a-fA-F0-9][a-fA-F0-9])");
   private static final Pattern encodedCharsMulti = Pattern.compile("((%[a-fA-F0-9][a-fA-F0-9])+)");

   public static String decodePath(String path)
   {
      Matcher matcher = encodedCharsMulti.matcher(path);
      StringBuffer buf = new StringBuffer();
      CharsetDecoder decoder = Charset.forName(UTF_8).newDecoder();
      while (matcher.find())
      {
         decoder.reset();
         String decoded = decodeBytes(matcher.group(1), decoder);
         decoded = decoded.replace("\\", "\\\\");
         decoded = decoded.replace("$", "\\$");
         matcher.appendReplacement(buf, decoded);
      }
      matcher.appendTail(buf);
      return buf.toString();
   }

   private static String decodeBytes(String enc, CharsetDecoder decoder)
   {
      Matcher matcher = encodedChars.matcher(enc);
      ByteBuffer bytes = ByteBuffer.allocate(enc.length() / 3);
      while (matcher.find())
      {
         int b = Integer.parseInt(matcher.group(1), 16);
         bytes.put((byte) b);
      }
      bytes.flip();
      try
      {
         return decoder.decode(bytes).toString();
      }
      catch (CharacterCodingException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Encode '%' if it is not an encoding sequence
    *
    * @param string
    * @return
    */
   public static String encodeNonCodes(String string)
   {
      Matcher matcher = nonCodes.matcher(string);
      StringBuffer buf = new StringBuffer();


      // FYI: we do not use the no-arg matcher.find()
      //      coupled with matcher.appendReplacement()
      //      because the matched text may contain
      //      a second % and we must make sure we
      //      encode it (if necessary).
      int idx = 0;
      while (matcher.find(idx))
      {
         int start = matcher.start();
         buf.append(string.substring(idx, start));
         buf.append("%25");
         idx = start + 1;
      }
      buf.append(string.substring(idx));
      return buf.toString();
   }

   private static boolean savePathParams(String segment, StringBuffer newSegment, List<String> params)
   {
      boolean foundParam = false;
      // Regular expressions can have '{' and '}' characters.  Replace them to do match
      segment = PathHelper.replaceEnclosedCurlyBraces(segment);
      Matcher matcher = PathHelper.URI_TEMPLATE_PATTERN.matcher(segment);
      while (matcher.find())
      {
         foundParam = true;
         String group = matcher.group();
         // Regular expressions can have '{' and '}' characters.  Recover earlier replacement
         params.add(PathHelper.recoverEnclosedCurlyBraces(group));
         matcher.appendReplacement(newSegment, "_resteasy_uri_parameter");
      }
      matcher.appendTail(newSegment);
      return foundParam;
   }

   /**
    * Keep encoded values "%..." and template parameters intact i.e. "{x}"
    *
    * @param segment
    * @param encoding
    * @return
    */
   private static String encodeValue(String segment, String[] encoding)
   {
      ArrayList<String> params = new ArrayList<String>();
      boolean foundParam = false;
      StringBuffer newSegment = new StringBuffer();
      if (savePathParams(segment, newSegment, params))
      {
         foundParam = true;
         segment = newSegment.toString();
      }
      String result = encodeFromArray(segment, encoding, false);
      result = encodeNonCodes(result);
      segment = result;
      if (foundParam)
      {
         segment = pathParamReplacement(segment, params);
      }
      return segment;
   }

   /**
    * Encode via <a href="http://ietf.org/rfc/rfc3986.txt">RFC 3986</a>.  PCHAR is allowed allong with '/'
    * <p/>
    * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
    * sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
    * / "*" / "+" / "," / ";" / "="
    * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
    */
   public static String encodePathAsIs(String segment)
   {
      return encodeFromArray(segment, pathEncoding, true);
   }

   /**
    * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
    *
    * @param segment
    * @return
    */
   public static String encodePathSaveEncodings(String segment)
   {
      String result = encodeFromArray(segment, pathEncoding, false);
      result = encodeNonCodes(result);
      return result;
   }

   /**
    * Encode via <a href="http://ietf.org/rfc/rfc3986.txt">RFC 3986</a>.  PCHAR is allowed allong with '/'
    * <p/>
    * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
    * sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
    * / "*" / "+" / "," / ";" / "="
    * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
    */
   public static String encodePathSegmentAsIs(String segment)
   {
      return encodeFromArray(segment, pathSegmentEncoding, true);
   }

   /**
    * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
    *
    * @param segment
    * @return
    */
   public static String encodePathSegmentSaveEncodings(String segment)
   {
      String result = encodeFromArray(segment, pathSegmentEncoding, false);
      result = encodeNonCodes(result);
      return result;
   }


   /**
    * Encodes everything of a query parameter name or value.
    *
    * @param nameOrValue
    * @return
    */
   public static String encodeQueryParamAsIs(String nameOrValue)
   {
      return encodeFromArray(nameOrValue, queryNameValueEncoding, true);
   }

   /**
    * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
    *
    * @param segment
    * @return
    */
   public static String encodeQueryParamSaveEncodings(String segment)
   {
      String result = encodeFromArray(segment, queryNameValueEncoding, false);
      result = encodeNonCodes(result);
      return result;
   }

   public static String encodeFragmentAsIs(String nameOrValue)
   {
      return encodeFromArray(nameOrValue, queryNameValueEncoding, true);
   }

   private static String encodeFromArray(String segment, String[] encodingMap, boolean encodePercent)
   {
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < segment.length(); i++)
      {
         if (!encodePercent && segment.charAt(i) == '%')
         {
            result.append(segment.charAt(i));
            continue;
         }
         int idx = segment.charAt(i);
         String encoding = encode(idx, encodingMap);
         if (encoding == null)
         {
            result.append(segment.charAt(i));
         }
         else
         {
            result.append(encoding);
         }
      }
      return result.toString();
   }

   /**
    * @param zhar        integer representation of character
    * @param encodingMap encoding map
    * @return URL encoded character
    */
   private static String encode(int zhar, String[] encodingMap)
   {
      String encoded;
      if (zhar < encodingMap.length)
      {
         encoded = encodingMap[zhar];
      }
      else
      {
         try
         {
            encoded = URLEncoder.encode(Character.toString((char) zhar), UTF_8);
         }
         catch (UnsupportedEncodingException e)
         {
            throw new RuntimeException(e);
         }
      }
      return encoded;
   }

   private static String pathParamReplacement(String segment, List<String> params)
   {
      StringBuffer newSegment = new StringBuffer();
      Matcher matcher = PARAM_REPLACEMENT.matcher(segment);
      int i = 0;
      while (matcher.find())
      {
         String replacement = params.get(i++);
         // double encode slashes, so that slashes stay where they are 
         replacement = replacement.replace("\\", "\\\\");
         replacement = replacement.replace("$", "\\$");
         matcher.appendReplacement(newSegment, replacement);
      }
      matcher.appendTail(newSegment);
      segment = newSegment.toString();
      return segment;
   }

   /**
    * decode an encoded map
    *
    * @param map
    * @return
    */
   public static MultivaluedHashMap<String, String> decode(MultivaluedHashMap<String, String> map)
   {
       MultivaluedHashMap<String, String> decoded = new MultivaluedHashMap<String, String>();
      for (Map.Entry<String, List<String>> entry : map.entrySet())
      {
         List<String> values = entry.getValue();
         for (String value : values)
         {
            try
            {
               decoded.add(URLDecoder.decode(entry.getKey(), UTF_8), URLDecoder.decode(value, UTF_8));
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
      }
      return decoded;
   }

   public static MultivaluedHashMap<String, String> encode(MultivaluedHashMap<String, String> map)
   {
       MultivaluedHashMap<String, String> decoded = new MultivaluedHashMap<String, String>();
      for (Map.Entry<String, List<String>> entry : map.entrySet())
      {
         List<String> values = entry.getValue();
         for (String value : values)
         {
            try
            {
               decoded.add(URLEncoder.encode(entry.getKey(), UTF_8), URLEncoder.encode(value, UTF_8));
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
      }
      return decoded;
   }

   public static String decode(String string)
   {
      try
      {
         return URLDecoder.decode(string, UTF_8);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException(e);
      }
   }

}
