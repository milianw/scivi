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

import javax.vecmath.GMatrix;
import javax.vecmath.GVector;
import javax.vecmath.MismatchedSizeException;

/**
 * A least-squares regression, also known as a regularized
 * least squares classification. 
 * 
 * @author Greg Dennis (gdennis@mit.edu)
 */
public final class Regression {

    private Regression() {}
    
    /**
     * Performs a least squares regression for the specified data matrix
     * (one data point in each column), and returns a representer function
     * fit to the data. The data point in column i is assumed to have the
     * value of the ith element in the values vector. The specified kernel
     * is used for the regression and the specified lambda is the penalty
     * factor on the complexity of the solution.
     * 
     * <p>Given the kernel matrix K, the identity matrix I, and the values
     * vector y, the returned representer function has the following vector
     * c of coefficients:
     * 
     * <blockquote>
     * c = (K - &lambda;I)<sup>-1</sup>y
     * </blockquote>
     */
    public static Representer solve(
            GMatrix data, GVector values, Kernel kernel, double lambda) {
        
        int numPoints = data.getNumCol();
        if (numPoints != values.getSize()) {
            throw new MismatchedSizeException();
        }
        
        // calculate the coefficients c
        GMatrix k = kernelMatrix(data, kernel);

        // subtract lambda * I from k
        for (int i = 0; i < numPoints; i++) {
        	double kElem = k.getElement(i, i);
            k.setElement(i, i, kElem - lambda);
        }
        
        k.invert();
        GVector c = new GVector(numPoints);
        c.mul(k, values);
        
        return new Representer(kernel, data, c);
    }
    
    /**
     * Returns a kernel matrix for the specified data matrix
     * (each column contains a data point) and the specified kernel.
     * The element (i, j) in the returned matrix is the kernel
     * evaluated for the data points in columns i and j in the data.
     */
    public static GMatrix kernelMatrix(GMatrix data, Kernel kernel) {
        int rows = data.getNumRow(), cols = data.getNumCol();
        GMatrix k = new GMatrix(cols, cols);
        GVector x1 = new GVector(rows);
        GVector x2 = new GVector(rows);
        
        // kernels are symmetric, so we only need calculate
        // every {i, j} combination, not permutation.
        for(int i = 0; i < cols; i++) {
            data.getColumn(i, x1);
            // set the diagonal
            k.setElement(i, i, kernel.eval(x1, x1));
            
            for(int j = i + 1; j < cols; j++) {
                data.getColumn(j, x2);
                double val = kernel.eval(x1, x2);
                k.setElement(i, j, val);
                k.setElement(j, i, val);
            }
        }
        
        return k;
    }
}
