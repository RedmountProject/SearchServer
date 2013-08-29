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

import com.sp.Daemon.SearchHandler;
import com.sp.Objects.Arguments;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author amaury
 */
public class Sorter {

    Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    private final SolrDocumentList SolrEmptyDoc;

    public Sorter() {
        SolrEmptyDoc = getSolrEmptyDoc();
    }

    private SolrDocumentList getSolrEmptyDoc() {
        try {
            LOG.debug("Getting Empty Solr Document");

            SolrServer solr = new HttpSolrServer(SearchHandler.solr_url);

            QueryResponse Response = solr.query(
                    new SolrQuery("product_id:999999"));

            LOG.debug("Query Execution Time : " + Response.getQTime());
            if (Response.getResults().isEmpty()) {
                LOG.info("Query Executed, But Returned Empty Results");
                return null;
            }
            LOG.debug("Query Executed, Returning Results");
            SolrDocumentList list = Response.getResults();
            return list;
        } catch (SolrServerException ex) {
            LOG.error("mmh : " + ex);
        }
        return null;
    }

    public SolrDocumentList sort(
            QueryResponse Response,
            HashMap<Object, Arguments> pSort_stack) {

        SolrDocumentList invoke_ret_docs;
        SolrDocumentList ret_docs = new SolrDocumentList();

        try {
            for (Map.Entry<Object, Arguments> entry :
                    pSort_stack.entrySet()) {
                String method_name = (String) entry.getKey();
                Arguments arguments = entry.getValue();


                while (!arguments.isEmpty()) {
                    Object object = arguments.pop();
                    Method method =
                            this.getClass().getDeclaredMethod(
                            method_name,
                            new Class[]{
                                Object[].class, QueryResponse.class});
                    invoke_ret_docs = (SolrDocumentList) method.invoke(
                            this,
                            (Object) object,
                            Response);
                    
                    ret_docs.addAll(invoke_ret_docs);
                }
            }
            
        } catch (IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException |
                NoSuchMethodException |
                SecurityException ex) {
            LOG.error("mmh ", ex);
        }
        ret_docs = Utils.DedupByBookId(ret_docs);
        return ret_docs;
    }

    private SolrDocumentList EmptyDoc(
            Object[] pArgs,
            QueryResponse Response) {

        return SolrEmptyDoc;


    }

    private SolrDocumentList Docs(
            Object[] pArgs,
            QueryResponse Response) {

        SolrDocumentList docs = Response.getResults();

        if (docs.isEmpty()) {
            return null;
        }


        return docs;
    }

    private SolrDocumentList Title(
            Object[] pArgs,
            QueryResponse Response) {
        SolrDocumentList docs = Response.getResults();
        SolrDocumentList temp_docs;


        if (pArgs.length > 1) {
            temp_docs = Keyword(pArgs, Response);
        } else {            
            temp_docs = Utils.getTop(docs, (ArrayList<String>)((Object[]) pArgs[0])[0], (ArrayList<String>)((Object[]) pArgs[0])[1]);
        }
        LOG.info("SolrDocList Size in Sorter : " + temp_docs.size());
        return temp_docs;
    }

    private SolrDocumentList Keyword(
            Object[] pArgs,
            QueryResponse Response) {
        try {
            String method_name = (String) pArgs[0];
            String field = (String) pArgs[1];

            Method method = this.getClass().getDeclaredMethod(
                    method_name, new Class[]{
                        String.class,
                        QueryResponse.class});

            return (SolrDocumentList) method.invoke(this, field, Response);
        } catch (IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException ex) {
            LOG.error("mmh : " + ex);
        } catch (NoSuchMethodException | SecurityException ex) {
            LOG.error("mmh : " + ex);
        }
        return null;
    }

    private SolrDocumentList author_match(
            final String pField,
            final QueryResponse Response) {



        SolrDocumentList docs = Response.getResults();

        if (docs.isEmpty()) {
            return null;
        }

        HashMap<String, Integer> product_ids = new HashMap<>();
        Pattern pattern = Pattern.compile("\\.NA");
        Matcher match;
        for (Iterator<SolrDocument> it = docs.iterator(); it.hasNext();) {
            SolrDocument solrDocument = it.next();

            String ProductId = solrDocument.getFieldValue("product_id").toString();
            Map<String, List<String>> HighlightedProduct = Response.getHighlighting().get(ProductId);
            if (HighlightedProduct != null && !HighlightedProduct.isEmpty()) {
                List<String> highlighted_field = HighlightedProduct.get(pField);
                if (highlighted_field != null && !highlighted_field.isEmpty()) {
                    for (String Keyword : highlighted_field) {
                        Keyword = Keyword.replaceAll("<\\/em>", "");
                        match = pattern.matcher(Keyword);
                        if (match.find()) {
                            LOG.info("AuthorName Found : " + Keyword);
                            product_ids.put(ProductId, 1);
                        }
                    }

                }
            }
        }

        final List<Entry<String, Integer>> entries = new ArrayList<>(product_ids.entrySet());

        // Tri de la liste sur la valeur de l'entrée
        Collections.sort(entries, integer_comp);

        return Dedup_Relate_Integer(entries, docs);
    }

    private SolrDocumentList nb_match(
            final String pField,
            final QueryResponse Response) {



        SolrDocumentList docs = Response.getResults();

        if (docs.isEmpty()) {
            return null;
        }

        HashMap<String, Integer> product_id_weight = new HashMap<>();
        for (Iterator<SolrDocument> it = docs.iterator(); it.hasNext();) {
            SolrDocument solrDocument = it.next();

            product_id_weight.putAll(extractHighlightsCount(Response, solrDocument, pField));

        }

        final List<Entry<String, Integer>> entries = new ArrayList<>(product_id_weight.entrySet());

        // Tri de la liste sur la valeur de l'entrée
        Collections.sort(entries, integer_comp);



        return Dedup_Relate_Integer(entries, docs);
    }
    private Comparator double_comp = new Comparator<Entry<String, Double>>() {

        public int compare(final Entry<String, Double> e1, final Entry<String, Double> e2) {
            return e2.getValue().compareTo(e1.getValue());
        }
    };
    private Comparator integer_comp = new Comparator<Entry<String, Integer>>() {

        public int compare(final Entry<String, Integer> e1, final Entry<String, Integer> e2) {
            return e2.getValue().compareTo(e1.getValue());
        }
    };

    private SolrDocumentList sum_weight(
            final String pField,
            final QueryResponse Response) {




        SolrDocumentList docs = Response.getResults();

        if (docs.isEmpty()) {
            return null;
        }

        HashMap<String, Double> product_id_weight = new HashMap<>();
        for (Iterator<SolrDocument> it = docs.iterator(); it.hasNext();) {
            SolrDocument solrDocument = it.next();

            product_id_weight.putAll(extractHighlightsWeights(Response, solrDocument, pField));

        }

        final List<Entry<String, Double>> entries = new ArrayList<>(product_id_weight.entrySet());

        // Tri de la liste sur la valeur de l'entrée
        Collections.sort(entries, double_comp);

        return Dedup_Relate_Double(entries, docs);
    }

    private HashMap<String, Integer> extractHighlightsCount(QueryResponse Response, SolrDocument o, String pField) {
        HashMap<String, Integer> t = new HashMap<>();

        String ProductId = o.getFieldValue("product_id").toString();
        Map<String, List<String>> HighlightedProduct = Response.getHighlighting().get(ProductId);
        if (HighlightedProduct != null && !HighlightedProduct.isEmpty()) {
            List<String> highlighted_field = HighlightedProduct.get(pField);
            if (highlighted_field != null && !highlighted_field.isEmpty()) {
                if (pField.equals("title")) {
                    int title_cut = 0;
                    for (String string : highlighted_field) {
                        int lastIndex = 0;
                        while (lastIndex != -1) {

                            lastIndex = string.indexOf("</em>", lastIndex);

                            if (lastIndex != -1) {
                                title_cut++;
                                lastIndex += "</em>".length();
                            }
                        }
                    }

                    LOG.info("Adding product_id: " + ProductId + " title nb_match : " + title_cut);
                    t.put(ProductId, title_cut);
                } else {
                    t.put(ProductId, highlighted_field.size());
                }
            }
        }

        return t;
    }

    private HashMap<String, Double> extractHighlightsWeights(QueryResponse Response, SolrDocument o, String pField) {
        HashMap<String, Double> t = new HashMap<>();

        String ProductId = o.getFieldValue("product_id").toString();
        Double weight = 0.0;
        Map<String, List<String>> HighlightedProduct = Response.getHighlighting().get(ProductId);
        if (HighlightedProduct != null && !HighlightedProduct.isEmpty()) {
            List<String> highlighted_field = HighlightedProduct.get(pField);
            if (highlighted_field != null && !highlighted_field.isEmpty()) {
                for (String Keyword : highlighted_field) {
                    Keyword = Keyword.replaceAll("<\\/em>", "");
                    weight += Utils.getWeightDouble(Keyword);

                }
                t.put(ProductId, weight);
            }
        }

        return t;
    }

    private SolrDocumentList Dedup_Relate_Double(List<Entry<String, Double>> entries, SolrDocumentList docs) {

        for (Iterator<Entry<String, Double>> it = entries.iterator(); it.hasNext();) {
            Entry<String, Double> entry = it.next();
            if (entry.getValue() == 0) {
                entries.remove(entry);
            }
        }

        SolrDocumentList return_docs = new SolrDocumentList();
        ArrayList<String> product_ids = new ArrayList<>();
        ArrayList<String> book_ids = new ArrayList<>();

        for (Iterator<Entry<String, Double>> it = entries.iterator(); it.hasNext();) {
            Entry<String, Double> entry = it.next();
            String product_id = entry.getKey();


            for (Iterator<SolrDocument> it1 = docs.iterator(); it1.hasNext();) {
                SolrDocument solrDocument = it1.next();
                String solr_product_id = solrDocument.getFieldValue("product_id").toString();
                String solr_book_id = solrDocument.getFieldValue("book_id").toString();

                if (!product_ids.contains(solr_product_id)
                        && product_id.equals(solr_product_id)
                        && !book_ids.contains(solr_book_id)) {
                    book_ids.add(solr_book_id);
                    product_ids.add(solr_product_id);
                    return_docs.add(solrDocument);
                }
            }
        }
        return return_docs;
    }

    private SolrDocumentList Dedup_Relate_Integer(List<Entry<String, Integer>> entries, SolrDocumentList docs) {

        for (Iterator<Entry<String, Integer>> it = entries.iterator(); it.hasNext();) {
            Entry<String, Integer> entry = it.next();
            if (entry.getValue() == 0) {
                entries.remove(entry);
            }
        }

        SolrDocumentList return_docs = new SolrDocumentList();
        ArrayList<String> product_ids = new ArrayList<>();
        ArrayList<String> book_ids = new ArrayList<>();

        for (Iterator<Entry<String, Integer>> it = entries.iterator(); it.hasNext();) {
            Entry<String, Integer> entry = it.next();
            String product_id = entry.getKey();

            for (Iterator<SolrDocument> it1 = docs.iterator(); it1.hasNext();) {
                SolrDocument solrDocument = it1.next();

                String solr_product_id = solrDocument.getFieldValue("product_id").toString();
                String solr_book_id = solrDocument.getFieldValue("book_id").toString();


                if (!product_ids.contains(solr_product_id)
                        && product_id.equals(solr_product_id)
                        && !book_ids.contains(solr_book_id)) {
                    book_ids.add(solr_book_id);
                    product_ids.add(solr_product_id);
                    return_docs.add(solrDocument);
                }
            }
        }


        return return_docs;
    }
}
