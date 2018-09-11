package net.maxsmr.copyutil.utils;

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

    public static boolean charsEqual(Character one, Character another, boolean ignoreCase) {
        if (ignoreCase) {
            one = one != null ? Character.toUpperCase(one) : null;
            another = another != null ? Character.toUpperCase(another) : null;
        }
        return one != null ? one.equals(another) : another == null;
    }
}
