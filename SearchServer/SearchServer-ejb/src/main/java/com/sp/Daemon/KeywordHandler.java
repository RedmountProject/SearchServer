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
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author amaury
 */
public class KeywordHandler extends Thread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(KeywordHandler.class);
    private static ServerSocket KeywordServerSocket;
    public static int keyword_port = 6668;

    public KeywordHandler(Configuration conf) {
        keyword_port = Integer.parseInt(conf.getProperty("keyword_port"));
    }

    @Override
    public void run() {
        try {
            launchKeywordServer();
            acceptRequestLoop();
        } catch (Exception ex) {
            LOG.trace("", ex);
        }
    }

    protected static void launchKeywordServer() throws Exception {
        KeywordServerSocket = new ServerSocket(keyword_port);
        KeywordServerSocket.setReuseAddress(true);
    }

    protected static void acceptRequestLoop() throws Exception {
        ExecutorService es = Executors.newCachedThreadPool();
        Socket KeywordClientSocket;
        KeywordThread ks;

        for (;;) {
            KeywordClientSocket = KeywordServerSocket.accept();
            ks = new KeywordThread(KeywordClientSocket);
            es.execute(ks);
        }
    }

    private static class KeywordThread extends Thread implements Runnable {
        private static Socket KeywordClientSocket;

        public KeywordThread(Socket s) {
            KeywordClientSocket = s;
        }

        
        @Override
        public void run() {
            try {
                String Output = "";
                String input = ReicvInput().trim();
                if(input.equals("")){
                    KeywordClientSocket.close();
                    return;
                }
                LOG.info("Keyword Request " + input);
                Output = UnitexParser.getKeywords(input, UUID.randomUUID().hashCode());
                Send_TCP(Output.trim());
                input = null;
                Output = null;
                KeywordClientSocket.close();
                finalize();
            } catch (Throwable ex) {
                LOG.error("", ex);
            } 
        }

        private static BufferedReader inFromServer;
        private static synchronized String ReicvInput()
                throws Exception {
            inFromServer = new BufferedReader(
                    new InputStreamReader(KeywordClientSocket.getInputStream(), "UTF8"));
            return inFromServer.readLine();
        }
        
        private static BufferedWriter outToClient;
        public static synchronized void Send_TCP(String output) 
                throws IOException {
            outToClient = new BufferedWriter(
                    new OutputStreamWriter(KeywordClientSocket.getOutputStream(), "UTF-8"));
            outToClient.write(output);
            LOG.info("Output : " + output);
            outToClient.flush();
        }
    }
}
