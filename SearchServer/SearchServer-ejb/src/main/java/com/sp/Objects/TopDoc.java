/*  Copyright (C) 2013 BRISOU Amaury

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.sp.Objects;

import java.util.*;

/**
 *
 * @author amaury
 *
 * NEVER EDIT THIS OBJECT : EXTENDS YOUR OWN OBJECT FROM THIS ONE INSTEAD
 */
public class TopDoc implements Comparable<TopDoc> {

    
    public Integer product_id;
    public double sum = new Double(0);
    
    public Integer MatchedKeywordsNumber = 0;
    protected ArrayList<Double> Weights;

    public TopDoc(String product_id) {
        this.Weights = new ArrayList();
        this.product_id = Integer.parseInt(product_id);

    }

    public void addWeight(double Weight) {
        MatchedKeywordsNumber++;
        Weights.add(Weight);
    }

    public void AddUp() {
        double p;
        sum = new Double(0);
        for (Iterator<Double> it = Weights.iterator(); it.hasNext();) {
            p = it.next();
            sum += p;

        }
    }
    
    @Override
    public int compareTo(TopDoc t) {

        return t.MatchedKeywordsNumber - MatchedKeywordsNumber;
    }

    
}
