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

import com.sp.Parser.Configuration;
import com.sp.Parser.UnitexParser;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;  
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amaury
 */
public class SearchHandler extends Thread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SearchHandler.class);
    public static int search_port = 6666;
    public static String solr_url = "http://192.168.1.28:8983/solr/SolrCatalog";

    

    public SearchHandler(Configuration conf) {
        search_port = Integer.parseInt(conf.getProperty("search_port"));
        solr_url = conf.getProperty("solr_url");
        LOG.info("solrUrl : " + solr_url);
    }

    @Override
    public void run() {
        ServerSocket SearchServerSocket;        
        try {
            SearchServerSocket = launchSearchServer();
            acceptRequestLoop(SearchServerSocket);
        } catch (Exception ex) {
            LOG.error("Error in Main Thread", ex);
        }
    }

    public static ServerSocket launchSearchServer() throws Exception {
        ServerSocket SearchServerSocket = new ServerSocket(search_port);
        SearchServerSocket.setReuseAddress(true);
        return SearchServerSocket;
    }

    public void acceptRequestLoop(ServerSocket SearchServerSocket) throws Exception {
        ExecutorService es = Executors.newCachedThreadPool();
        Socket SearchClientSocket;
        QueryHandler client;
        
        UserList ClientList = new UserList(10);
        
        for (;;) {
            SearchClientSocket = SearchServerSocket.accept();
            LOG.info("New Search Request");
            
            client = ClientList.getPending();
            client.setSocket(SearchClientSocket);
            
            es.submit(client);
        }
    }
    
    private static class UserList extends ArrayList<QueryHandler> {

        public UserList(int initialCapacity) {
            super(initialCapacity);
            for (int i = 0; i < initialCapacity; i++) {
                this.add(new QueryHandler());
            }
        }

        
        public QueryHandler getPending() {

            for (QueryHandler obj : this) {
                if (obj.pending) {
                    obj.pending = false;
                    return obj;
                }
            }
            QueryHandler obj = new QueryHandler();
            this.add(obj);
            return obj;
        }
    }
}
