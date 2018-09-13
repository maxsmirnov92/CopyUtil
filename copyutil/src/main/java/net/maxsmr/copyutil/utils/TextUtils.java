package net.maxsmr.copyutil.utils;

import static net.maxsmr.copyutil.utils.CompareUtils.charsEqual;

public class TextUtils {

    /**
     * Returns true if the string is null or 0-length.
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param tokens an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     */
    public static String join(CharSequence delimiter, Object[] tokens) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token: tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(token);
        }
        return sb.toString();
    }

    public static String appendOrReplaceChar(CharSequence source, Character what, String to, boolean ignoreCase, boolean appendOrReplace) {

        if (TextUtils.isEmpty(source) ||  TextUtils.isEmpty(to)) {
            return "";
        }

        StringBuilder newStr = new StringBuilder();

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (charsEqual(c, what, ignoreCase)) {
                if (appendOrReplace) {
                    newStr.append(c);
                    newStr.append(to);
                } else {
                    newStr.append(to);
                }
            } else {
                newStr.append(c);
            }
        }

        return newStr.toString();
    }

    public static String trim(CharSequence cs) {
        return trim(cs, true, true);
    }

    public static String trim(CharSequence cs, boolean fromStart, boolean fromEnd) {

        if (cs == null) {
            return "";
        }

        String str = cs.toString();

        int len = str.length();
        int st = 0;

        if (fromStart) {
            while ((st < len) && (str.charAt(st) <= ' ')) {
                st++;
            }
        }
        if (fromEnd) {
            while ((st < len) && (str.charAt(len - 1) <= ' ')) {
                len--;
            }
        }
        return ((st > 0) || (len < str.length())) ? str.substring(st, len) : str;
    }
}
