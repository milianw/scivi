/*
 * Drej
 * Copyright (c) 2005 Greg Dennis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.gregdennis.drej;

import java.util.Random;

import javax.vecmath.GMatrix;
import javax.vecmath.GVector;
import javax.vecmath.MismatchedSizeException;

/**
 * Utility methods for matrices and vectors.
 * 
 * @author Greg Dennis (gdennis@mit.edu)
 */
public final class Matrices {
    
    private final static Random RAND = new Random();

    private Matrices() {}
    
    /**
     * Returns the distance between the two specified vectors:
     * <blockquote>
     * &#8741;x1 - x2&#8741;
 	 * </blockquote>
 	 * 
 	 * @throws MismatchedSizeException if vectors have different sizes
     */
    public static double distance(GVector x1, GVector x2) {
        return Math.sqrt(distanceSquared(x1, x2));
    }
    
    /**
     * Returns the squared distance between the two specified vectors:
     * <blockquote>
     * &#8741;x1 - x2&#8741;&sup2
 	 * </blockquote>
 	 * 
 	 * @throws MismatchedSizeException if vectors have different sizes
     */
    public static double distanceSquared(GVector x1, GVector x2) {
        if (x1.getSize() != x2.getSize()) {
            throw new MismatchedSizeException("x1 size: " + x1.getSize() +
                                              "; x2 size: " + x2.getSize());
        }
        
        double distSquared = 0;
        for(int i = 0; i < x1.getSize(); i++) {
            double diff = x1.getElement(i) - x2.getElement(i);
            distSquared += diff * diff;
        }
        return distSquared;
    }
    
    /**
     * Maps the given function to each column in the points
     * matrix and returns the vector of values.
     */
    public static GVector mapCols(Function fun, GMatrix points) {
        if (fun == null)  throw new NullPointerException("fun");        
        int rows = points.getNumRow();
        int cols = points.getNumCol();
        GVector values = new GVector(cols);
        
        for(int i = 0; i < cols; i++) {
            GVector x = new GVector(rows);
            points.getColumn(i, x);
            values.setElement(i, fun.eval(x));
        }
        
        return values;
    }

    /**
     * Maps the given function to each row in the points
     * matrix and returns the vector of values.
     */
    public static GVector mapRows(Function fun, GMatrix points) {
        if (fun == null)  throw new NullPointerException("fun");        
        int rows = points.getNumRow();
        int cols = points.getNumCol();
        GVector values = new GVector(rows);
        
        for(int i = 0; i < rows; i++) {
            GVector x = new GVector(cols);
            points.getRow(i, x);
            values.setElement(i, fun.eval(x));
        }
        
        return values;
    }
    
    /**
     * Returns a matrix with the specified number of rows and columns
     * where each element is randomly chosen from a uniform distribution
     * on the interval [0, 1].
     */
    public static GMatrix randomUniformMatrix(int rows, int cols) {
        GMatrix m = new GMatrix(rows, cols);
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                m.setElement(i, j, RAND.nextDouble());
            }
        }
        return m;
    }
    
    /**
     * Returns a matrix with the specified number of rows and columns
     * where each element is randomly chosen from a Gaussian ("normal")
     * distribution with mean 0.0 and standard deviation 1.0.
     */
    public static GMatrix randomGaussianMatrix(int rows, int cols) {
        GMatrix m = new GMatrix(rows, cols);
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                m.setElement(i, j, RAND.nextGaussian());
            }
        }
        return m;
    }

    /**
     * Returns a vector of the given size where each element is randomly
     * chosen from a uniform distribution on the interval [0, 1].
     */
    public static GVector randomUniformVector(int size) {
        GVector v = new GVector(size);
        for(int i = 0; i < size; i++) {
            v.setElement(i, RAND.nextDouble());
        }
        return v;
    }
    
    /**
     * Returns a vector of the given size where each element is randomly
     * chosen from a Gaussian ("normal") distribution with mean 0.0 and
     * standard deviation 1.0.
     */
    public static GVector randomGaussianVector(int size) {
        GVector v = new GVector(size);
        for(int i = 0; i < size; i++) {
            v.setElement(i, RAND.nextGaussian());
        }
        return v;
    }
}
