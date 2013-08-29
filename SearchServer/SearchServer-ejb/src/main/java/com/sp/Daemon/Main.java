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

import com.sp.Exception.ExceptionHandler;
import com.sp.Parser.Configuration;
import com.sp.Parser.UnitexParser;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.servlet.ServletContextHandler;
//import org.eclipse.jetty.servlet.ServletHolder;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final long serialVersionUID = 1L;
    private static final Configuration conf = new Configuration();
    public static int admin_port;

    static {
        try {
            System.out.println(System.getProperty("user.dir")+conf.getProperty("unitex_shared_library"));
            System.load(System.getProperty("user.dir")+conf.getProperty("unitex_shared_library"));
        } catch (UnsatisfiedLinkError e) {
            LOG.error("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }
    

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        LOG.info("library path = "+System.getProperty("java.library.path"));
        LOG.info("classpath = "+System.getProperty("java.class.path"));

        try {


            UnitexParser.loadDictionariesAndAlphabets();

//            launchAdminServer();

            ExecutorService es = Executors.newFixedThreadPool(2);

            es.submit(new SearchHandler(conf));
            es.submit(new KeywordHandler(conf));

        } catch (IOException ex) {
            new ExceptionHandler(ex);
        } catch (UnsatisfiedLinkError ex) {
            new ExceptionHandler(ex);
        } catch (Exception ex) {
            new ExceptionHandler(ex);
        }
    }
//    private static void launchAdminServer() throws IOException, InterruptedException, Exception {
//        Server server = new Server(admin_port);
//
//        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//        context.setContextPath("/");
//        server.setHandler(context);
//
//        context.addServlet(new ServletHolder(new AdminQueryHandler()), "/");
//        context.addServlet(new ServletHolder(new UpdateAuteurDictionary()), "/update_auteur");
//
//        server.start();
//
//    }
}
