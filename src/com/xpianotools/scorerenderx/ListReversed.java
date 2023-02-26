 /*************************************************************************
  * 
  * XPianoTools
  * http://www.xpianotools.com
  * 
  *  [2014] XPianoTools 
  *  All Rights Reserved.
  * 
  * NOTICE:  All information contained herein is, and remains
  * the property of XPianoTools and its suppliers,
  * if any.
  * 
  * Developer: Georgy Osipov
  * 	developer@xpianotools.com
  * 	gaosipov@gmail.com
  * 
  * 2014-15-08
  *************************************************************************/

package com.xpianotools.scorerenderx;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ListReversed<T> implements Iterable<T> {
    private final List<T> original;

    public ListReversed(List<T> original) {
        this.original = original;
    }

    public Iterator<T> iterator() {
        final ListIterator<T> i = original.listIterator(original.size());

        return new Iterator<T>() {
            public boolean hasNext() { return i.hasPrevious(); }
            public T next() { return i.previous(); }
            public void remove() { i.remove(); }
        };
    }

    public static <T> ListReversed<T> reversed(List<T> original) {
        return new ListReversed<T>(original);
    }
}