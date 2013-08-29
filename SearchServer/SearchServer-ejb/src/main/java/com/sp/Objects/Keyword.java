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

import com.sp.Parser.Utils;
import java.util.Objects;

/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
/**
 *
 * @author amaury
 */
public class Keyword extends Object {

    public String input;
    public String output;
    public String UserInput;
    public String lemma;
    public String concat;

    public Keyword(String pOutput, String pLemma) {
        output = pOutput;
        lemma = pLemma;
        concat = output + "." + lemma;
    }

    public Keyword(String pInput, String pOutput, String pLemma) {
        input = pInput;//Utils.Strip(pInput.trim());
        output = pOutput;//Utils.Strip(pOutput.trim());
        lemma = pLemma.trim();
        UserInput = input;
        concat = output + "." + lemma;
    }

    public Keyword(String UserInput) {
        this.UserInput = UserInput;
        this.input = UserInput;
        this.output = UserInput;
        this.concat = UserInput +"."+"*";
    }

    public void setUserInput(String pUserInput) {
        this.UserInput = pUserInput;
    }

    public String getUserInput() {
        return UserInput;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public void dump() {
        System.out.println("BEGIN KEYWORD");
        System.out.println("Input : " + input);
        System.out.println("Output : " + output);
        System.out.println("Lemma : " + lemma);
        System.out.println("Concat : " + concat);
        System.out.println("END KEYWORD");

    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Keyword) {
            Keyword toCompare = (Keyword) o;
            if(this.output.equalsIgnoreCase(toCompare.output)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(this.output);
        return hash;
    }
}
