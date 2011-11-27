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
 * A representer function with a kernel K(x1, x2), a set of training
 * data points (d1, d2, &#133;, dn), and a vector (c1, c2, &#133;, cn)
 * of coefficients.
 * 
 * <blockquote>
 * &fnof;(x) = &sum; c<sub>i</sub>K(d<sub>i</sub>, x)
 * </blockquote>
 * 
 * @author Greg Dennis (gdennis@mit.edu)
 */
public final class Representer implements Function {
    
    private final Kernel kernel;
    private final GVector[] points;
    private final GVector coeffs;
    /** for returning to avoid rep exposure. */
    private final GVector copyCoeffs;

    /**
     * Constructs a new representer with the specified kernel,
     * data matrix, and coefficients. The data matrix is expected
     * to contain one data point in each column.
     * 
     * @throws MismatchedSizeException
     *   if number of data columns differs from number of coefficients.
     */
    public Representer(Kernel kernel, GMatrix data, GVector coeffs) {
        if (kernel == null) throw new NullPointerException("kernel");
        if (data.getNumCol() != coeffs.getSize()) {
            throw new MismatchedSizeException();
        }
        
        this.kernel = kernel;
        this.points = new GVector[data.getNumCol()];
        this.coeffs = new GVector(coeffs);
        this.copyCoeffs = new GVector(coeffs);
        
        for(int i = 0; i < data.getNumCol(); i++) {
            this.points[i] = new GVector(data.getNumRow());
            data.getColumn(i, points[i]);
        }
    }
    
    /**
     * Returns a copy of the vector of coefficients.
     */
    public GVector coeffs() {
        return copyCoeffs;
    }
    
    public double eval(GVector x) {
        double sum = 0;
        for(int i = 0; i < coeffs.getSize(); i++) {
            sum += coeffs.getElement(i) * kernel.eval(points[i], x);
        }
        return sum;
    }

}
