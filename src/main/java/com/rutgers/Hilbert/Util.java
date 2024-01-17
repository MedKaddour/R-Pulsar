package com.rutgers.Hilbert;

/**
* This is used to implement miscellaneous tools need it the space filling curve.
* 
* @author  Eduard Giber Renart
* @version 1.0
*/

final class Util {

    private Util() {
        // prevent instantiation
    }

    // visible for testing
    static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    static boolean allZero(int[] a) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != 0)
                return false;
        }
        return true;
    }

    /**
     * Returns the least value x where min(a,b) <= x < max(a,b) and x has the
     * maximum number of trailing zeroes in its binary representation.
     * 
     * @param a
     *            first value
     * @param b
     *            second value
     * @return most significant (in terms of divisors of powers of 2) between a
     *         and b
     */
    static long mostSignificantBetween(long a, long b) {
        if (a > b) {
            return mostSignificantBetween(b, a);
        } else if (a == b) {
            return a;
        } else {
            long x = a == 0 ? 1 : a;
            int bit = 0;
            while (x < b) {
                if ((x & (1 << bit)) == 0) {
                    bit++;
                } else {
                    long y = x + (1 << bit);
                    if (y < b) {
                        bit++;
                        x = y;
                    } else {
                        break;
                    }
                }
            }
            return x;
        }
    }

}
