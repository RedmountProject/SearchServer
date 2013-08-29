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
import com.sp.Manager.QueryManager;
import com.sp.Objects.Keyword;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amaury
 */
public class SolrQueryBuilder extends SolrQuery {

    private static Logger LOG = LoggerFactory.getLogger(
            SolrQueryBuilder.class);
    private SolrQuery cleared_parameters;
    private final int NUM_ROWS = 101;
    private final String SOLR_URL = SearchHandler.solr_url;
    private ArrayList<Object> keywords = new ArrayList<>();

    /**
     * Initialize the Object with Filters { Countries, Prices } And create a
     * backup parameters in order to fallback during search processing
     *
     * @param obj
     */
    public SolrQueryBuilder() {
        super();
        cleared_parameters = new SolrQuery();
    }

    public void clear() {
        this.keywords.clear();
        cleared_parameters.clear();
        super.clear();
    }

    public void Update(QueryManager obj) {

        setFilters(obj.filters);
        if (!obj.hasKeywords() && !obj.hasGenres() && !obj.hasPublisher()) {
            for (String Ukw : obj.SplitUserInput) {
                this.keywords.add(new Keyword(Ukw));
            }
        } else {
            this.keywords = (ArrayList<Object>) obj.keywords.clone();
        }
        updateParams();
    }

    public final void updateParams() {
        LOG.info("Backing Up Default Query");
        cleared_parameters = this.getCopy();
    }

    public void clearParams() {
        LOG.info("Clearing Query (Keeping Filters)");
        super.clear();
        this.add(cleared_parameters);
        LOG.info("Query is now : " + this.toNamedList());
    }

    public void UpdateWithGenre(ArrayList<Object> genres, Boolean pClear) {
        LOG.info("Updating Query to fetch Genres ");
        String Query = GenerateFieldQuery("genre_id", genres);
        UpdateQuery(Query, pClear);
        setSortField("publishing_date", SolrQuery.ORDER.desc);

    }

    //This method could be avoided but it improves
    //coding abstraction from QueryGatherer point of view
    public void UpdateWithPublisher(
            ArrayList<Object> publishers,
            Boolean pClear) {
        LOG.info("Updating Query to fetch Publishers ");
        String Query = GenerateFieldQuery("publisher_id", publishers);
        UpdateQuery(Query, pClear);
        setSortField("publishing_date", SolrQuery.ORDER.desc);
    }

    public void QueryById(String pField, int pId, Boolean pClear) {
        LOG.info("Updating Query on searchable :"
                + pField + " : " + pId);
        String Query = pField + ":" + pId;
        UpdateQuery(Query, pClear);
        addFilterQuery("searchable:true");
    }

    public void UpdateWithKeyword(
            String pField,
            String pMethod_name,
            String pLemma,
            Boolean pClear) {
        try {
            LOG.info("Updating Query with " + (pField) + "(s)");
            LOG.info("Generating " + (pField) + " Query With "+
                    pMethod_name + pLemma);

            Method method = Keyword.class.getDeclaredMethod(
                    pMethod_name, new Class[]{});

            String Query = "", kw;
            int len = keywords.size();
            for (int i = 0; i < len; i++) {
                kw = (String) method.invoke(keywords.get(i), new Object[]{});

                if (i != len - 1) {
                    Query += pField + ":(" + kw.trim() + pLemma + ") OR ";
                } else {
                    Query += pField + ":(" + kw.trim() + pLemma + ")";
                }
            }
            LOG.info(pField + "(s) Query Generation Result : " + Query);
            UpdateQuery(Query, pClear);
            setHighlightField(pField, 15);

        } catch (IllegalAccessException | IllegalArgumentException ex) {
            LOG.trace("mmh :" + ex);
        } catch (InvocationTargetException ex) {
            LOG.trace("mmh :" + ex);
        } catch (NoSuchMethodException | SecurityException ex) {
            LOG.trace("mmh :" + ex);
        }
    }

    private void setHighlightField(String pField, int SnippetsNum) {
        LOG.info("Setting Highlight Field " + pField);
        if (getHighlight()) {
            LOG.info("Query Already Contains Highlighted Fields");
            addHighlightField(pField);
            LOG.info("Query Highlighted");
        } else {
            setParam("hl.fl", pField);
        }
        setHighlight(true);
        //Number of matched keywords to put in the highlighted queue
        setHighlightSnippets(SnippetsNum);
        
    }

    private void setFilters(ArrayList<String> filters) {
        LOG.info("Adding Filters to Query");

        setRows(NUM_ROWS);
        setParam("indent", "on");

        for (String filter : filters) {
            LOG.info("Filter : " + filter);
            addFilterQuery(filter);
        }

        LOG.info("Default Query is now : " + this.toNamedList());

    }

    public QueryResponse ExecuteQuery()
            throws SolrServerException, MalformedURLException {
        LOG.info("Executing Query " + this.getQuery()
                + " On Solr at " + SOLR_URL);

        SolrServer solr = new HttpSolrServer(SOLR_URL);
        String[] f = this.getFilterQueries();
        for (String string : f) {
            LOG.info(string);
        }
        
        QueryResponse Response = solr.query((SolrQuery) this);

        LOG.info("Query Execution Time : " + Response.getQTime());
        if (Response.getResults().isEmpty()) {
            LOG.info("Query Executed, But Returned Empty Results");
            return null;
        }
        LOG.info("Query Executed, Returning Results");
        return Response;

    }

    private static String GenerateFieldQuery(String Field, ArrayList<?> pObject) {

        String Query = "";
        for (int i = 0; i < pObject.size(); i++) {
            if (i != pObject.size() - 1) {
                Query += Field + ":("
                        + (Object) pObject.get(i).toString().trim()
                        + ") OR ";
            } else {
                Query += Field + ":("
                        + (Object) pObject.get(i).toString().trim()
                        + ")";
            }
        }


        return Query;
    }

    private void UpdateQuery(String pQuery, boolean pClear) {
        if (pClear) {
            AddToClearQuery(pQuery);
        } else {
            AddToQuery(pQuery);
        }
    }

    /**
     * Add to the Query, clearing every param added before except filters
     *
     * @param pQuery
     * @see SolrQueryBuilder.AddToQuery
     */
    private void AddToClearQuery(String pQuery) {
        LOG.info("Adding To Clear Query");
        clearParams();
        String temp = this.get("q") != null ? this.get("q") + " OR " : "";
        this.set("q", temp + pQuery);
        LOG.info("New Query : " + this.getQuery());
    }

    /**
     * Add to the Query, concatening with other params added before.
     *
     * @param pQuery
     * @see SolrQueryBuilder.AddToClearQuery
     */
    private void AddToQuery(String pQuery) {
        LOG.info("Adding To Query");
        String temp = this.get("q") != null ? this.get("q") + " OR " : "";
        this.set("q", temp + pQuery);
        LOG.info("New Query : " + this.getQuery());
    }
}
