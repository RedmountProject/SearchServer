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

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author amaury
 */
public class Configuration extends java.util.Properties {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    public Configuration() {
        super();

        try {

            // Get hold of the path to the properties file
            // (Maven will make sure it's on the class path)
            java.net.URL url = Configuration.class.getClassLoader().getResource("config.cfg");

            // Load the file
            load(url.openStream());
        } catch (IOException ex) {
            LOG.trace("mmh config.cfg missing !", ex);
        }
    }
}
