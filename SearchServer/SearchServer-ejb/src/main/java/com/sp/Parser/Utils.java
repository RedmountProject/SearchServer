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
import com.sp.Objects.TopDoc;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.text.Normalizer;
import java.util.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
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
 * @author amaury
 */
public class Utils {

    private static Logger LOG = LoggerFactory.getLogger(
            Utils.class);

    public static synchronized void Send_TCP(
            Socket s, String JsonFormattedResponse) throws IOException {

        BufferedWriter outToClient = new BufferedWriter(
                new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
        outToClient.write(JsonFormattedResponse);
        outToClient.flush();
    }

    public static String BuildResponse(SolrDocumentList TargetDoc) {

        if (TargetDoc != null) {
            return JsonParser.parse(TargetDoc);

        }
        return null;
    }

    public static SolrDocumentList SortListByProductId(ArrayList<String> TempQuery, SolrDocumentList list) {

        SolrDocumentList TempList = new SolrDocumentList();


        String doc_product_id;
        for (String product_id : TempQuery) {

            for (SolrDocument doc : list) {
                doc_product_id =
                        doc.getFieldValue("product_id").toString();

                if (doc_product_id.equalsIgnoreCase(product_id)) {
                    TempList.add(doc);
                    break;
                }
            }
        }



        return TempList;
    }

    public static SolrDocumentList DedupByBookId(
            SolrDocumentList SolrDocList) {

        SolrDocumentList TempList = new SolrDocumentList();
        ArrayList<String> TempStringList = new ArrayList<>();

        String doc_book_id;
        for (SolrDocument doc : SolrDocList) {
            doc_book_id = doc.getFieldValue("book_id").toString();
            if (!TempStringList.contains(doc_book_id)) {
                //System.out.println("doc_book_id " + doc_book_id);
                TempStringList.add(doc_book_id);
                TempList.add(doc);
            }
        }
        System.out.println("Templist size " + TempList.size());
        return TempList;
    }

    public static int Calculate_Keyword_Sum(
            List<String> highlighted_Keywords) {

        int Weightsum = 0;
        int weight;

        for (String Keyword : highlighted_Keywords) {
            weight = getWeight(Keyword);
            Weightsum += weight;
        }

        return Weightsum;
    }

    public static int getWeight(String Keyword) {
        int indexOfPipe = Keyword.indexOf("|");
        String WeightString = Keyword.substring(indexOfPipe + 1).trim();
        int weight = Integer.parseInt(WeightString);
        return weight;
    }

    public static Double getWeightDouble(String Keyword) {
        int indexOfPipe = Keyword.indexOf("|");
        String WeightString = Keyword.substring(indexOfPipe + 1).trim();
        Double weight = Double.parseDouble(WeightString);
        return weight;
    }
    private static final String[] InputReplace = {"é", "è", "ê", "ë", "û", "ù", "ü", "ï", "î", "à", "â", "ö", "ô", "ç"};
    private static final String[] OutputReplace = {"e", "e", "e", "e", "u", "u", "u", "i", "i", "a", "a", "o", "o", "c"};

    public static String Strip(String Input) {
//        Input = Input.replaceAll("(\\b|_)(a|o)u+x?\\b", "");
//        Input = Input.replaceAll("(\\b|_)l(e|a|es)\\b", "");
//        Input = Input.replaceAll("(\\b|_)(m|t|s)(on|a|es)\\b", "");
//        Input = Input.replaceAll("(\\b|_)une?\\b", "");
//        Input = Input.replaceAll("(\\b|_)es?t\\b", "");
//        Input = Input.replaceAll("(\\b|_)ce(lle)?s?\\b", "");
//        Input = Input.replaceAll("(\\b|_)d(es?|u)\\b", "");
//        Input = Input.replaceAll("(\\b|_)d(es?|u)\\b", "");
        Input = StringUtils.replaceEachRepeatedly(Input.toLowerCase(), InputReplace, OutputReplace);
        Input = StringEscapeUtils.escapeSql(Input);
        Input = Normalizer.normalize(Input.toLowerCase(), Normalizer.Form.NFD);
        return (Input.replaceAll("  *", " ")).trim();
        //return Input;
    }

    public static SolrDocumentList getTop(
            SolrDocumentList docs,
            ArrayList<String> prices, ArrayList<String> filters){
        SolrDocumentList SolrDocList = null;
        try {

            SolrDocument PivotDoc = docs.get(0); //store pId book to put it in the index 0 of the final SolrDocList

            Collection<Object> Top20Product_ids = PivotDoc.getFieldValues("top20"); //store pId book to put it in the index 0 of the final SolrDocList

            PivotDoc.removeFields("top20"); // get current book top20 with products ids
            PivotDoc.removeFields("keywords");

            Collection<Object> BackupTop20Product_ids = Top20Product_ids;

            String QueryString = "product_id:(";
            ArrayList<String> TempQuery = new ArrayList<>();

            for (Object product_id : BackupTop20Product_ids) {
                QueryString += product_id.toString() + " ";
                TempQuery.add(product_id.toString());
            }
            QueryString += ")";

            String prices_fields = "";
            for (String string : prices) {
                prices_fields += string + " ";

            }



            SolrQuery Query = new SolrQuery(QueryString);
            Query.setRows(101);
            for (String filter : filters) {
                LOG.info("Top20 Filtering : " + filter);
                Query.addFilterQuery(filter);
            }
            Query.setParam("fl", "product_id book_id author_searchable author_id format_name description title author_firstname"
                    + " file_size publishing_date author_lastname author_rank publisher_id publisher_name"
                    + " permalink nb_pages isbn " + prices_fields);

            SolrServer solr = new HttpSolrServer(SearchHandler.solr_url);

            QueryResponse response = solr.query(Query);

            SolrDocList = response.getResults();

            if (!SolrDocList.isEmpty()) {

                SolrDocList = Utils.SortListByProductId(TempQuery, SolrDocList);

                if (!SolrDocList.isEmpty()) {
                    SolrDocument temp = SolrDocList.get(0);

                    SolrDocList.set(0, PivotDoc);
                    SolrDocList.add(1, temp);
                }
            } else {
                SolrDocList.add(0, PivotDoc);
            }
        } catch (SolrServerException ex) {
            LOG.info("mmh : ", ex);
        }
        LOG.info("SolrDocList Size int getTop : " + SolrDocList.size());
        return SolrDocList;
    }
}
