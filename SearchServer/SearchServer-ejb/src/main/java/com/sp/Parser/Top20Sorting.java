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

import com.sp.Objects.TopDoc;
import java.util.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author amaury
 */
public class Top20Sorting {

    public ArrayList<TopDoc> TopDocList = new ArrayList<>();
    private SolrDocumentList Top20SolrDocList;
    private QueryResponse response;
    private final String field;

    private void Manage() {
        switch (field) {
            case "keywords":
                ManageKeywords();
                break;
            default:
                ManageField();
                break;
        }
    }

    private void ManageKeywords() {
        Iterator<SolrDocument> iter = Top20SolrDocList.iterator();

        while (iter.hasNext()) {

            SolrDocument resultDoc = iter.next();

            String ProductId = resultDoc.getFieldValue("product_id").toString();

            TopDoc topdoc = new TopDoc(ProductId);

            Map<String, List<String>> HighlightedProduct = response.getHighlighting().get(ProductId);
            if (HighlightedProduct != null && !HighlightedProduct.isEmpty()) {
                int sum = 0;

                List<String> highlighted_Keywords = HighlightedProduct.get("keywords");

                if (highlighted_Keywords != null && !highlighted_Keywords.isEmpty()) {
                    for (String Keyword : highlighted_Keywords) {
                        Double weight = Utils.getWeightDouble(Keyword);
                        topdoc.addWeight(weight);
                    }

                    topdoc.AddUp();
                    TopDocList.add(topdoc);
                }
            }
        }
    }

    public Top20Sorting(SolrDocumentList pTop20SolrDocList, QueryResponse pResponse, String pField) {
        Top20SolrDocList = pTop20SolrDocList;
        response = pResponse;
        field = pField;
    }

    public ArrayList<TopDoc> sort() {

        Manage();

        System.out.println(Top20SolrDocList.size());

        if (TopDocList.isEmpty()) {
            return TopDocList;
        }

        Collections.sort(TopDocList);

        cutTopList();

        for (TopDoc d : TopDocList) {
            System.out.println(d.MatchedKeywordsNumber + " " + d.sum + " " + d.product_id);
        }
        return TopDocList;

    }
    public Comparator<TopDoc> SumOrder = new Comparator<TopDoc>() {

        public int compare(TopDoc t1, TopDoc t2) {
            return Double.compare(t1.sum, t2.sum);
        }
    };

    private void cutTopList() {

        ArrayList<TopDoc> TempTopList = (ArrayList<TopDoc>) TopDocList.clone();
        TopDocList.clear();

        ArrayList<TopDoc> List = new ArrayList<>();

        int TempMatchNumber = ((TopDoc) TempTopList.get(0)).MatchedKeywordsNumber;

        for (Iterator it = TempTopList.iterator(); it.hasNext();) {
            TopDoc doc = (TopDoc) it.next();

            if (doc.MatchedKeywordsNumber == TempMatchNumber) {
                List.add(doc);
            } else {
                List = sortList(List);
                TopDocList.addAll(List);
                List.clear();
                TempMatchNumber = doc.MatchedKeywordsNumber;
            }
        }
        List = sortList(List);
        TopDocList.addAll(List);
    }

    private ArrayList<TopDoc> sortList(ArrayList<TopDoc> List) {

        Collections.sort(List, SumOrder);

        return List;
    }

    public void dump() {
        for (TopDoc d : TopDocList) {
            System.out.println("product_id : " + d.product_id);
            System.out.println("sum : " + d.sum);
            System.out.println("MatchedKeywordsNumber : " + d.MatchedKeywordsNumber);
        }
    }

    public void e() {
        System.exit(1);
    }

    private void ManageField() {
        Iterator<SolrDocument> iter = Top20SolrDocList.iterator();

        while (iter.hasNext()) {

            SolrDocument resultDoc = iter.next();

            String ProductId = resultDoc.getFieldValue("product_id").toString();

            TopDoc topdoc = new TopDoc(ProductId);

            Map<String, List<String>> HighlightedProduct = 
                    response.getHighlighting().get(ProductId);

            if (HighlightedProduct != null && !HighlightedProduct.isEmpty()) {
                List<String> highlighted_field = HighlightedProduct.get(field);

                if (highlighted_field != null && !highlighted_field.isEmpty()) {
                    topdoc.AddUp();
                    TopDocList.add(topdoc);
                }
            }
        }
    }
}
