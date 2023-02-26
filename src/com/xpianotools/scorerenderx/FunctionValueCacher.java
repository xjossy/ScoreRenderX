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

import android.util.SparseArray;

public class FunctionValueCacher {
	private int revision = 0;
	
	public void updated() {
		revision++;
	}
	
	protected class CachedValue<T> {
		private int revision = FunctionValueCacher.this.revision;
		private T val = null;
		T get() {
			if( revision!=FunctionValueCacher.this.revision ) {
				val = null;
			}
			return val;
		}
		void set(T t) {
			revision=FunctionValueCacher.this.revision;
			val = t;
		}
	}
	
	protected class CachedValueF {
		private int revision = FunctionValueCacher.this.revision;
		private float val;
		float get() {
			return val;
		}
		float set(float t) {
			revision=FunctionValueCacher.this.revision;
			return val = t;
		}
		boolean has() {
			return revision==FunctionValueCacher.this.revision;
		}
	}

	protected class Cache<T> {
		private int revision = FunctionValueCacher.this.revision;
		private SparseArray<T> values = new SparseArray<T>();
		T get(int hash) {
			if( revision!=FunctionValueCacher.this.revision ) {
				values.clear();
				revision=FunctionValueCacher.this.revision;
			}
			return values.get(hash);
		}
		void set(int hash, T val) {
			values.put(hash, val);
		}
	}
}
