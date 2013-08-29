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

/**
 *
 * @author amaury
 */
public class Publisher extends Object {

    private Integer publisher_id;
    public String publisher_name;

    public Publisher() {
    }

    public Publisher(Integer pPublisher, String pPublisher_name) {
        publisher_id = pPublisher;
        publisher_name = pPublisher_name;
    }

    public String toString() {
        return publisher_id.toString();
    }

    public void dump() {
        if(publisher_id != null){
            System.out.println("Publisher : " + publisher_id);
            System.out.println("Publisher Name : " + publisher_name);
        }
        
    }
}
