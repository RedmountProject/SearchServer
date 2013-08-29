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
package com.sp.Daemon;

import com.sp.Parser.Sorter;
import com.sp.Exception.ExceptionHandler;
import com.sp.Manager.QueryGatherer;
import com.sp.Manager.QueryManager;
import com.sp.Parser.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryHandler extends Thread implements Runnable {

    Logger LOG = LoggerFactory.getLogger(QueryHandler.class);
    public boolean pending = true;
    private Socket ClientSocket;
    private String dateFormat = "dd/MM/yy H:mm:ss";
    private QueryManager Query;
    private Sorter Sorter;
    private final QueryGatherer Gatherer;

    public QueryHandler(Socket pClientSocket) {
        this.ClientSocket = pClientSocket;
        Query = new QueryManager();
        Gatherer = new QueryGatherer();
        Sorter = new Sorter();
        
    }

    public QueryHandler() {
        Query = new QueryManager();
        Gatherer = new QueryGatherer();
        Sorter = new Sorter();
    }

    public void setSocket(Socket pClientSocket) {
        this.ClientSocket = pClientSocket;
    }

    private synchronized String ReicvUserInput() throws Exception {

        java.text.SimpleDateFormat formater =
                new java.text.SimpleDateFormat(dateFormat);
        java.util.Date date = new java.util.Date();


        BufferedReader inFromServer = new BufferedReader(
                new InputStreamReader(ClientSocket.getInputStream(), "UTF8"));
        //reading query
        return inFromServer.readLine();
    }

    private void ForgeAndSend(SolrDocumentList TargetDoc)
            throws IOException {

        String Response;
        Response = Utils.BuildResponse(TargetDoc);
        if (Response != null && !Response.isEmpty()) {
            Utils.Send_TCP(ClientSocket, Response);
        }
    }

    public void run() {

        try {

            String UserInput = ReicvUserInput();

            LOG.info("Connection from IP : "
                    + ClientSocket.getRemoteSocketAddress());


            if (!UserInput.isEmpty()) { //virtualizing query string

                LOG.info("Received data : " + UserInput);


                if (Query.Parse(UserInput, ClientSocket, this.getId())
                        == null) {
                    ClientSocket.close();
                    return;
                }

                Query.parseKeywords(UserInput);

                Query.Decide();

                if (Query.call_stack.isEmpty()) {
                    return;
                }
                QueryResponse Response = Gatherer.Gather(Query);

                SolrDocumentList docs = new SolrDocumentList();
                if (!Response.getResults().isEmpty()) {


                    docs = Sorter.sort(
                            Response,
                            Query.sort_stack);

                    LOG.info("Match Count : " + docs.size());
                }
                
                ForgeAndSend(docs);

            }

        } finally {
            LOG.info(Thread.currentThread().getName() + " Request Managed");
            try {
                this.Query.clear();
                this.Gatherer.clear();
                this.pending = true;
                
                ClientSocket.close();
                //Clearing call_stack and more

            } catch (IOException ex) {
                new ExceptionHandler(ex);
            }
            return;
        }

    }
}
