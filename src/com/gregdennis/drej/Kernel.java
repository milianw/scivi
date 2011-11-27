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
 * A Mercer kernel function. Every implementation should be a
 * continuous and positive-definite function.
 * 
 * @author Greg Dennis (gdennis@mit.edu)
 */
public interface Kernel {

    /**
     * Evaluates the kernel function at the specified points.
     */
    public double eval(GVector x1, GVector x2);
    
}
