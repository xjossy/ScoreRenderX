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

import java.io.Serializable;


public class Pair<T,S> implements Serializable {
	private static final long serialVersionUID = -8472718012872205543L;
	public T first;
	public S second;
	
	public Pair() {
		this(null,null);
	}
	public Pair(T first, S second) {
		this.first = first;
		this.second = second;
	}

	public Pair<S,T> swap() {
		return new Pair<S,T>(second,first);
	}
	public boolean notNull() {
		return first != null && second != null;
	}
}
