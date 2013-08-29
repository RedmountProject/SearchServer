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
public class Genre extends Object {
    
    public Integer genre_id;
    public String genre_name;
    
    public Genre(Integer pGenre_id, String pGenre_name){
        genre_id = pGenre_id;
        genre_name = pGenre_name;
    }
    
    public String toString(){
        return genre_id.toString();
    }

    public void dump() {
        System.out.println("BEGIN GENRE");
        System.out.println("Genre ID : "+genre_id);
         System.out.println("Genre NAME : "+genre_name);
        System.out.println("END GENRE");
    }
}
