package net.maxsmr.copyutil.utils;

import java.util.Arrays;

public class ArgsParser {

    public static Pair<Integer, String> findArg(String[] argsNames, String[] args, int index) {
        if (argsNames == null || argsNames.length == 0) {
            throw new IllegalArgumentException("Incorrect args names: " + Arrays.toString(argsNames));
        }
        if (index < 0 || index >= argsNames.length) {
            throw new IllegalArgumentException("Incorrect arg name index: " + index);
        }
        return args != null?
                Predicate.Methods.findWithIndex(Arrays.asList(args), element -> element != null && element.equals(argsNames[index]))
                : null;
    }

    public static String getPairArg(String args[], Pair<Integer, String> pair) {
        return args != null && pair != null && pair.first != null && pair.first < args.length - 1? args[pair.first + 1] : null;
    }
}
