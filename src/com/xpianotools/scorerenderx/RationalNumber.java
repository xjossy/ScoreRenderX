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

public class RationalNumber implements Cloneable, Comparable<RationalNumber> {
    public static final RationalNumber Zero = new RationalNumber(0,1);
	public static final RationalNumber One  = new RationalNumber(1,1);
	public static final RationalNumber MAX_VALUE  = new RationalNumber(Long.MAX_VALUE,1);

    public long numerator;
    public long denominator;
    
    public RationalNumber clone() {
    	return new RationalNumber(numerator, denominator);
    }

    public RationalNumber(int nominator, int denominator) {
        this((long) nominator, (long) denominator);
    }

    public RationalNumber(long nominator, long denominator) {
    	if(denominator == 0) throw new IllegalArgumentException();
    	
        this.numerator = nominator;
        this.denominator = denominator;
        
        simplify();
    }

    public String getStringValue() {
        return this.numerator + "/"
                + this.denominator;
    }

    public double getDoubleValue() {
        return (double) this.numerator / (double) this.denominator;
    }
    
    public void set(RationalNumber r) {
    	this.numerator = r.numerator;
    	this.denominator = r.denominator;
    }
    
    public RationalNumber multiplyThis(RationalNumber r) {
    	this.numerator   *= r.numerator;
    	this.denominator *= r.denominator;
    	simplify();
    	return this;
    }
    
    public RationalNumber divideThis(RationalNumber r) {
    	if( r.numerator==0 ) throw new IllegalArgumentException();
    	this.numerator   *= r.denominator;
    	this.denominator *= r.numerator;
    	simplify();
    	return this;
    }
    
    public RationalNumber multiplyThis(long r) {
    	this.numerator   *= r;
    	simplify();
    	return this;
    }
    
    public RationalNumber divideThis(long r) {
    	if( r==0 ) throw new IllegalArgumentException();
    	this.denominator *= r;
    	simplify();
    	return this;
    }
    
    public RationalNumber addThis(RationalNumber r) {
    	this.numerator   = this.numerator*r.denominator + r.numerator*this.denominator;
    	this.denominator*= r.denominator;
    	simplify();
    	return this;
    }
    
    public RationalNumber subtractThis(RationalNumber r) {
    	this.numerator   = this.numerator*r.denominator - r.numerator*this.denominator;
    	this.denominator*= r.denominator;
    	simplify();
    	return this;
    }
    
    public RationalNumber addThis(long r) {
    	this.numerator   = this.numerator + r*this.denominator;
    	simplify();
    	return this;
    }
    
    public RationalNumber subtractThis(long r) {
    	this.numerator   = this.numerator - r*this.denominator;
    	simplify();
    	return this;
    }

    public RationalNumber multiply(RationalNumber r) {
        RationalNumber res = clone();
        return res.multiplyThis(r);
    }

    public RationalNumber divide(RationalNumber r) {
    	RationalNumber res = clone();
        return res.divideThis(r);
    }
    
    public RationalNumber multiply(long r) {
        RationalNumber res = clone();
        return res.multiplyThis(r);
    }

    public RationalNumber divide(long r) {
    	RationalNumber res = clone();
        return res.divideThis(r);
    }

    public RationalNumber add(RationalNumber r) {
    	RationalNumber res = clone();
        return res.addThis(r);
    }

    public RationalNumber subtract(RationalNumber r) {
    	RationalNumber res = clone();
        return res.subtractThis(r);
    }
    
    public RationalNumber add(long r) {
    	RationalNumber res = clone();
        return res.addThis(r);
    }

    public RationalNumber subtract(long r) {
    	RationalNumber res = clone();
        return res.subtractThis(r);
    }

    public void simplify() {
        long gcd = gcd(numerator,denominator);
        numerator   /= gcd;
        denominator /= gcd;
    }
    
    public int sign(long s) {
    	return (int) ((s >> 63) - (-s >> 63));
    }
    
    public int sign() {
        return sign(numerator) * sign(denominator);
    }

    // Euclidean algorithm to find greatest common divisor
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);

        while (a!=0) {
            b = b%a;
            a += b;
            b = a - b;
            a = a - b;
        }

        return b;
    }

    
    public int compareTo(RationalNumber r) {
    	return sign(this.numerator*r.denominator - r.numerator*this.denominator) * sign(r.denominator) * sign(this.denominator);
    }
    
    public int compareTo(long r) {
    	return sign(this.numerator - r*this.denominator) * sign(this.denominator);
    }
    
    public boolean equals(RationalNumber r) {
    	return compareTo(r) == 0;
    }
    
    public boolean equals(long r) {
    	return compareTo(r) == 0;
    }
    
    public boolean equals(Object r) {
    	if(r instanceof RationalNumber) return compareTo((RationalNumber)r) == 0;
    	if(r instanceof Long) return compareTo((long)(Long)r) == 0;
    	if(r instanceof Integer) return compareTo((long)(Integer)r) == 0;
    	return false;
    }
    
    public static RationalNumber max(RationalNumber x,RationalNumber y) {
    	return x.compareTo(y) >= 0 ? x : y;
    }
    
    public static RationalNumber min(RationalNumber x,RationalNumber y) {
    	return x.compareTo(y) <= 0 ? x : y;
    }
}