package net.maxsmr.copyutil.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Predicate<V> {

    boolean apply(V element);

    class Methods {

        public static <V> boolean contains(Collection<V> elements,  Predicate<V> predicate) {
            return find(elements, predicate) != null;
        }


        public static <V> V find(Collection<V> elements,  Predicate<V> predicate) {
            V result = null;
            if (elements != null) {
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result = elem;
                        break;
                    }
                }
            }
            return result;
        }


        public static <V> List<V> filter(Collection<V> elements,  Predicate<V> predicate) {
            List<V> result = new ArrayList<>();
            if (elements != null) {
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result.add(elem);
                    }
                }
            }
            return result;
        }
    }
}