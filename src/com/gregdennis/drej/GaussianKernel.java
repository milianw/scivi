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

import javax.vecmath.GVector;

/**
 * A Gaussian {@link com.gregdennis.drej.Kernel kernel} of the following form:<br>
 * 
 * <blockquote>
 * K(x1, x2) = exp(&#8210;&gamma;&sup2; &middot;  &#8741;x1 - x2&#8741;&sup2;)
 * </blockquote>
 * 
 * @author Greg Dennis (gdennis@mit.edu)
 */
public class GaussianKernel implements Kernel {
    
    private final double gamma, a;

    /**
     * Construct a Gaussian kernel with the specified value for &gamma;.
     */
    public GaussianKernel(double gamma) {
        this.gamma = gamma;
        this.a = -gamma * gamma;
    }
    
    /**
     * Returns the value of &gamma;.
     */
    public double gamma() {
        return gamma;
    }
    
    public double eval(GVector x1, GVector x2) {
        return Math.exp(a * Matrices.distanceSquared(x1, x2));
    }

}
