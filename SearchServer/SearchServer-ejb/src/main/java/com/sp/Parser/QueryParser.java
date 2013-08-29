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
package com.sp.Parser;

import com.sp.Manager.QueryManager;
import com.sp.Objects.Genre;
import com.sp.Objects.Keyword;
import com.sp.Objects.Publisher;
import java.net.Socket;
import java.util.*;

/*
 * This class parse the Unitex Daemon Ouput resulting from User Query.
 *
 * @author amaury
 */
public class QueryParser {

    private long id = 0;
    public ArrayList<Object> keywords = new ArrayList<>();
    public ArrayList<Object> genres = new ArrayList<>();
    public ArrayList<Object> publishers = new ArrayList<>();
    public ArrayList<String> filters = new ArrayList<>(5);
    public ArrayList<String> country_prices = new ArrayList<>(5);
    public int request_type;
    public int author_title_id = -1;
    public int searchable;
    public String[] SplitUserInput;

    public QueryParser() {
    }

    private void addGenre(Genre pGenre) {
        genres.add(pGenre);
    }

    public String getGenreId() {
        for (Iterator<Object> it = genres.iterator(); it.hasNext();) {
            Genre genre = (Genre) it.next();
            return genre.toString();
        }
        return "";
    }

    private void addPublisher(Publisher pPublisher) {
        publishers.add(pPublisher);
    }

    private void addKeyword(Keyword pKeyword) {
        if (!keywords.contains(pKeyword)) {
            keywords.add(pKeyword);
        }
    }

    public boolean hasKeywords() {
        return keywords.size() > 0 ? true : false;
    }

    public boolean hasGenres() {
        return genres.size() > 0 ? true : false;
    }

    public boolean hasPublisher() {
        return publishers.size() > 0 ? true : false;
    }

    public QueryManager parseKeywords(String UserInput) {
        String UnitexOutput;
        //UnitexOutput = UnitexParser.getKeywords(UserInput, id);
        extractAndInsertFields(UserInput);
        return (QueryManager) this;
    }

    public QueryManager Parse(String UserInput, Socket Socket, long Id) {
//        addSplitInput(Utils.Strip(UserInput).trim());
        addSplitInput((UserInput).trim());

        setQueryType(UserInput);

        ProcessFilters(UserInput);

        String UnitexOutput;
        UnitexOutput = UnitexParser.Manage(UserInput, id);

        extractAndInsertFields(UnitexOutput);

        UserInput = UserInput.split("_")[2];
        UserInput = (Utils.Strip(UserInput)).trim();

        if (UserInput.isEmpty()) {
            return null;
        }

        return (QueryManager) this;
    }

    public String[] getFilters(String UserInput) {
        return UserInput.split("_");
    }

    public String[] getCountries(String UserInput) {
        return UserInput.split("_");
    }

    public void ProcessFilters(String UserInput) {

        String trash[];

        trash = getCountries(UserInput);
        String country = trash[3];
        String default_country = trash[4];
        String world = trash[5];



        trash = getFilters(UserInput);
        int price = Integer.parseInt(trash[0]);
        String format_filter = trash[1];

        country_prices.add(country + "_EUR_TTC_c");
        country_prices.add(default_country + "_EUR_TTC_c");
        country_prices.add(world + "_EUR_TTC_c");


        filters.add(country + "_EUR_TTC_c:[0.00,EUR TO " + price + ".00,EUR]");
        filters.add(default_country + "_EUR_TTC_c:[0.00,EUR TO " + price + ".00,EUR]");
        filters.add(world + "_EUR_TTC_c:[0.00,EUR TO " + price + ".00,EUR]");

        int epub = Character.getNumericValue(format_filter.charAt(0));
        int pdf = Character.getNumericValue(format_filter.charAt(1));
        int mobile = Character.getNumericValue(format_filter.charAt(2));
        int drm = Character.getNumericValue(format_filter.charAt(3));


        ArrayList<String> format_filters = new ArrayList<>();

        if (!(epub == 1 && pdf == 1 && mobile == 1 && drm == 1)) {
            if (epub == 1) {
                if (drm == 0) {
                    format_filters.add("4_1 OR 4_3");
                } else {
                    format_filters.add("4_*");
                }
            }
            if (pdf == 1) {
                if (drm == 0) {
                    format_filters.add("6_1 OR 6_3");
                } else {
                    format_filters.add("6_*");
                }
            }
            if (mobile == 1) {
                if (drm == 0) {
                    format_filters.add("7_1 OR 7_3");
                } else {
                    format_filters.add("7_*");
                }
            }
        }

        String qr_str = "";
        int i = 1;
        for (String f : format_filters) {
            if (i < format_filters.size()) {
                qr_str += f + " OR ";
            } else {
                qr_str += f;
            }
            i++;
        }

        if (!qr_str.equals("")) {
            filters.add("format_id:(" + qr_str + ")");
        }
    }

    public void extractAndInsertFields(String UnitexOutput) {

        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(Arrays.asList(UnitexOutput.split("\n")));

        for (String out : temp) {
            if (out.contains("PUBLISHER")) {
                insertPublisher(out);
            }
            if (out.contains("GENRE")) {
                insertGenres(out);
            }
            if (out.contains("MOT-CLE")) {
                insertKeywords(out);
            }

        }
    }

    public void insertKeywords(String out) {
        ArrayList<String> s = getKeywordFields(out);

        String input_clean = Utils.Strip(s.get(0)).trim();
        String output_clean = Utils.Strip(s.get(1)).trim();
        if (input_clean.equals("") || output_clean.equals("")) {
            return;
        }
        System.out.println(input_clean);
        System.out.println(output_clean);
        Keyword pKw = new Keyword(input_clean, output_clean, s.get(2).trim());
        addKeyword(pKw);
    }

    public void setQueryType(String UserInput) {
        String f[] = UserInput.split("_");
        this.request_type = Integer.parseInt(f[6]);
        if (this.request_type < 2) {

            this.author_title_id = Integer.parseInt(f[7]);
            this.searchable = Integer.parseInt(f[8]);
        }
    }

    public ArrayList<String> getTitles(String s) {
        ArrayList<String> temp = new ArrayList<>();
        String g = s.split("=")[1];
        temp.addAll(Arrays.asList(g.split("_")));
        return temp;
    }

    public ArrayList<String> getAuthorFields(String s) {
        ArrayList<String> temp = new ArrayList<>();
        s = s.split("=")[1];
        temp.addAll(Arrays.asList(s.split("_")));
        return temp;
    }

    public ArrayList<String> getKeywordFields(String s) {

        ArrayList<String> temp = new ArrayList<>();
        s = s.split("=")[1];
        temp.addAll(Arrays.asList(s.split("_")));
        return temp;
    }

    public void insertGenres(String s) {
        s = s.split("=")[1].trim();
        String[] g = s.split("_");
        addGenre(new Genre(Integer.parseInt(g[0]), g[1]));
    }

    public void insertPublisher(String s) {
        s = s.split("=")[1].trim();
        String[] g = s.split("_");
        addPublisher(new Publisher(Integer.parseInt(g[0]), g[1]));
    }

    private void addSplitInput(String UserInput) {
        String[] s = UserInput.split("_");

        this.SplitUserInput = s[2].split(" ");
        
        for (String Ukw : SplitUserInput) {
            if(Ukw.equals("")) continue;
            this.keywords.add(new Keyword(Ukw));
        }
    }

    public void clear() {
        this.SplitUserInput = new String[]{};
        this.author_title_id = -1;
        this.keywords.clear();
        this.genres.clear();
        this.publishers.clear();
        this.filters.clear();
        this.country_prices.clear();
        this.request_type = -1;
        this.author_title_id = -1;
        this.searchable = -1;
    }
}
