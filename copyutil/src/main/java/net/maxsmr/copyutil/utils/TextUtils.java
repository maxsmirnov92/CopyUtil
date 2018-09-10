package net.maxsmr.copyutil.utils;



public class TextUtils {

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
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

    // TODO move
    public static String changeString(String source, char what, String to, boolean appendOrReplace) {

        if (TextUtils.isEmpty(source) ||  TextUtils.isEmpty(to)) {
            return "";
        }

        StringBuilder newStr = new StringBuilder();

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == what) {
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
}
