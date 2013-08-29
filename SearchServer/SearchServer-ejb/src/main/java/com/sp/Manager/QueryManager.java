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
package com.sp.Manager;

import com.sp.Objects.Arguments;
import com.sp.Parser.QueryParser;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amaury
 */
public class QueryManager extends QueryParser {

    public HashMap<Object, Arguments> call_stack = new HashMap<>(5);
    public HashMap<Object, Arguments> sort_stack = new HashMap<>(5);
    private static final Logger LOG = LoggerFactory.getLogger(QueryManager.class);

    public QueryManager() {
        super();
    }

    /**
     * Watch Out Stacking Order !!!
     *
     * @return HashMap<String, Arguments>
     * @throws Exception
     */
    public void Decide() throws Exception {
        LOG.info("Choosing Search Path");

        Arguments args = new Arguments();

        if (request_type >= 2) {
            if (hasKeywords()) {
                args.add(new Object[]{"getOutput", ".*"});
                args.add(new Object[]{"getUserInput", ".NA"});

                call_stack.put("Keyword", (Arguments) args.clone());

                args.clear();
                args.add(new Object[]{"getUserInput", ""});
                args.add(new Object[]{"getUserInput", ""});
                call_stack.put("Title", (Arguments) args.clone());


                EmptyDoc();

                args.clear();
                args.add(new Object[]{"sum_weight", "keywords"});
                args.add(new Object[]{"nb_match", "keywords"});
                args.add(new Object[]{"nb_match", "title"});
                args.add(new Object[]{"author_match", "keywords"});


                sort_stack.put("Keyword", (Arguments) args.clone());

            }

            if (hasGenres()) {
                args.clear();
                args.add(new Object[]{genres});
                call_stack.put("Genre", (Arguments) args.clone());

                Docs();
                EmptyDoc();

            }

            if (hasPublisher()) {
                args.clear();
                args.add(new Object[]{publishers});
                call_stack.put("Publisher", (Arguments) args.clone());

                Docs();
                EmptyDoc();

            }
        }

        if (request_type == 0) {
            if (searchable == 1) {
                args.clear();
                args.add(new Object[]{author_title_id});
                call_stack.put("Title", (Arguments) args.clone());

                args.clear();
                Object[] s = new Object[]{country_prices, filters};
                args.add(new Object[]{s});
                sort_stack.put("Title", (Arguments) args.clone());

            } else {
                args.clear();
                args.add(new Object[]{"getInput", ""});
                call_stack.put("Title", (Arguments) args.clone());

                EmptyDoc();

                args.clear();
                args.add(new Object[]{"nb_match", "title"});
                args.add(new Object[]{"sum_weight", "keywords"});
                args.add(new Object[]{"nb_match", "keywords"});
                sort_stack.put("Keyword", (Arguments) args.clone());

            }
        }

        if (request_type == 1) {
            args.clear();
            args.add(new Object[]{author_title_id});
            call_stack.put("Author", (Arguments) args.clone());

            Docs();
            EmptyDoc();

        }

        if (call_stack.isEmpty()) {
            args.add(new Object[]{"getUserInput", ".*"});
            args.add(new Object[]{"getUserInput", ".NA"});

            call_stack.put("Keyword", (Arguments) args.clone());

            args.clear();
            args.add(new Object[]{"getUserInput", ""});
            args.add(new Object[]{"getUserInput", ""});
            call_stack.put("Title", (Arguments) args.clone());

            EmptyDoc();
            args.clear();

            args.add(new Object[]{"sum_weight", "keywords"});
            args.add(new Object[]{"nb_match", "keywords"});
            args.add(new Object[]{"nb_match", "title"});
            args.add(new Object[]{"author_match", "keywords"});

            sort_stack.put("Keyword", (Arguments) args.clone());
        }

        LOG.info("Path Choosen : " + call_stack.toString());
        LOG.info("Sort Path Choosen : " + sort_stack.toString());

    }

    private void EmptyDoc() {
        Arguments args = new Arguments();
        args.add(new Object[]{"", ""});
        sort_stack.put("EmptyDoc", (Arguments) args.clone());
    }

    private void Docs() {
        Arguments args = new Arguments();
        args.add(new Object[]{"", ""});
        sort_stack.put("Docs", (Arguments) args.clone());
    }

    public void clear() {
        super.clear();
        this.sort_stack.clear();
        this.call_stack.clear();
    }
}
