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
import com.sp.Parser.SolrQueryBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author amaury
 */
public class QueryGatherer {

    Logger LOG = LoggerFactory.getLogger(QueryGatherer.class);
    public String unitex_ret = "";
    private SolrQueryBuilder query_parameters;

    public QueryGatherer() {
        query_parameters = new SolrQueryBuilder();

    }

    public void clear(){
        this.query_parameters.clear();
    }
    
    public QueryResponse Gather(QueryManager pQuery)
            throws MalformedURLException {

        query_parameters.Update(pQuery);

        HashMap<Object, Arguments> pCall_stack = pQuery.call_stack;

        int RETRY_COUNT = 1;
        if (pCall_stack.get("Keyword") != null) {
            RETRY_COUNT += pCall_stack.get("Keyword").size();
        }

        QueryResponse Response = null;
        SolrDocumentList TargetDocs;
        Boolean clear = true;
        LOG.info("RETRY_COUNT " + RETRY_COUNT);
        try {
            while (RETRY_COUNT > 0) {
                LOG.info("Loop " + (RETRY_COUNT - RETRY_COUNT + 1));
                STACK_LOOP:
                for (Map.Entry<Object, Arguments> entry :
                        pCall_stack.entrySet()) {
                    String method_name = (String) entry.getKey();
                    Arguments arguments = entry.getValue();

                    while (!arguments.isEmpty()) {
                        Object object = arguments.pop();
                        Method method =
                                this.getClass().getDeclaredMethod(
                                method_name,
                                new Class[]{
                                    Object[].class,
                                    Boolean.class});

                        method.invoke(this, (Object) object, clear);
                        clear = false;
                    }



                }

                Response = query_parameters.ExecuteQuery();

                RETRY_COUNT--;
                clear = true;

                TargetDocs = Response.getResults();
                if (!isNull(TargetDocs)) {
                    break;
                }

            }
        } catch (IOException | SolrServerException ex) {
            LOG.error("mmh ", ex);
        } catch (IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException |
                NoSuchMethodException |
                SecurityException ex) {
            LOG.error("", ex);
        }
        query_parameters.clear();

        return Response;
    }

    private void Keyword(Object[] pArgs, Boolean pClear) {
        String kw_source_method = (String) pArgs[0];
        String lemma = (String) pArgs[1];

        query_parameters.UpdateWithKeyword("keywords",
                kw_source_method, lemma, pClear);
    }

    private void has_Nothing() throws Exception {
        LOG.info("hasNothing");
    }

    private void Title(Object[] pArgs, Boolean pClear) throws Exception {
        if (pArgs.length > 1) {
            String title_source_method = (String) pArgs[0];
            String lemma = (String) pArgs[1];
            query_parameters.UpdateWithKeyword("title",
                    title_source_method, lemma, pClear);
        } else {
            int product_id = (int) pArgs[0];
            query_parameters.QueryById("product_id", product_id, pClear);
        }
    }

    private void Author(Object[] pArgs, Boolean pClear) throws Exception {
        LOG.info("Author");
        int author_id = (int) pArgs[0];
        query_parameters.QueryById("author_id", author_id, pClear);

    }

    private void Genre(Object[] pArgs, Boolean pClear) throws Exception {
        LOG.info("Genres");
        ArrayList<Object> genres = (ArrayList<Object>) pArgs[0];
        query_parameters.UpdateWithGenre(genres, pClear);
    }

    private void Publisher(Object[] pArgs, Boolean pClear)
            throws Exception {
        LOG.info("Publisher");
        ArrayList<Object> publishers = (ArrayList<Object>) pArgs[0];
        query_parameters.UpdateWithPublisher(publishers, pClear);

    }

    private boolean isNull(SolrDocumentList doc) {
        return (doc == null || doc.isEmpty()) ? true : false;
    }
}