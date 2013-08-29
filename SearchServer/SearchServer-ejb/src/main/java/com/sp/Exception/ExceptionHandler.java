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
package com.sp.Exception;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author amaury
 */
public class ExceptionHandler extends Exception {
    
    private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
    public enum Errors {

        SolrServerException("Solr Server May Be Down"),
        UnsatisfiedLinkError("Couldn't Find Unitex Native Library"),
        ConnectException("Connection Refused");
        
        private String message;

        private Errors(final String message) {
            this.message = message;
        }

        public String toString() {
            // TODO Auto-generated method stub
            return message;
        }
    }

    public ExceptionHandler(IOException ex) {
        LOG.error(null, ex);
    }

    public ExceptionHandler(MalformedURLException ex) {
        LOG.error(null, ex);
    }

    public ExceptionHandler(UnsatisfiedLinkError ex) {
        LOG.error(Errors.UnsatisfiedLinkError.toString(), ex);
    }

    public ExceptionHandler(SolrServerException ex) {
        LOG.error(Errors.SolrServerException.toString(), ex);
    }

    public ExceptionHandler(ConnectException ex) {
        LOG.error("Connection to the Solr Server Refused", ex);
    }

    public ExceptionHandler(UnsupportedEncodingException ex) {
        LOG.error("Unsupported Encoding", ex);
    }

    public ExceptionHandler(Exception ex) {
        LOG.error(null, ex);
    }
    
    public ExceptionHandler(NumberFormatException  ex) {
         LOG.error("It's not an Integer");
    }
    
    public ExceptionHandler(IndexOutOfBoundsException  ex) {
         LOG.error("Your array is certainly Empty", ex);
    }
}
