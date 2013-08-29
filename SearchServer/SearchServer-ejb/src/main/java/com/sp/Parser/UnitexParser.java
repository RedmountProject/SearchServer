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

import com.sp.Exception.ExceptionHandler;
import fr.umlv.unitex.jni.UnitexJni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author amaury
 *
 */
public class UnitexParser {

    private static final Logger LOG = LoggerFactory.getLogger(UnitexParser.class);
    private static final String PFX = "$:";
    private static final String path = System.getProperty("user.dir") + "/classes/";

    public synchronized static void loadDictionariesAndAlphabets() throws Exception {

        String[] dictionaries = {"dic/sample-dlcf.bin", "dic/dela-fr-public.bin", "dic/auteurs.bin", "dic/stopwords.bin"};

        for (String dic : dictionaries) {
            UnitexJni.loadPersistentDictionary(path + dic);
            LOG.info("Loading :" + path + dic);
        }

        String[] graphs = {"graph/sanspapier.fst2", "graph/cdic.fst2"};

        for (String graph : graphs) {
            UnitexJni.loadPersistentFst2(path + graph);
            LOG.info("Loading : " + path + graph);
        }

        String[] alphabets = {"common/Alphabet.txt", "common/Alphabet_sort.txt", "common/Norm.txt"};

        for (String alphabet : alphabets) {
            UnitexJni.loadPersistentAlphabet(path + alphabet);
            LOG.info("Loading : " + path + alphabet);
        }

    }

    public synchronized static void ReloadAuteurDictionary() {
        UnitexJni.freePersistentDictionary(path + "dic/auteurs.bin");
        UnitexJni.loadPersistentDictionary(path + "dic/auteurs.bin");
        LOG.info("Reloading : " + path + "dic/auteurs.bin");
    }

    public static String Manage(String data, long id) {

        UnitexJni.setStdOutTrashMode(true);
        UnitexJni.setStdErrTrashMode(true);

        data = data.split("_")[2];
        return getKeywords(data, id);

    }

    public static String getKeywords(String data, long id) {

        String temp = PFX + path + id + "temp.txt";

        if (!UnitexJni.writeUnitexFileUtf(temp, data, false)) {
            LOG.error("Error virtualizing query string");
            return "";
        }
        if (UnitexJni.execUnitexTool(new String[]{"UnitexToolLogger", "Normalize", temp, "-r" + PFX + path + "common/Norm.txt"}) != 0) {
            LOG.error("Error in Normalize");
            return "";
        }
        
        if (UnitexJni.execUnitexTool(new String[]{"UnitexToolLogger", "Tokenize", PFX + path + id + "temp.snt", "-a" + PFX + path + "common/Alphabet.txt"}) != 0) {
            LOG.error("Error in Tokenize");
            return "";
        }
        if (UnitexJni.execUnitexTool(new String[]{"UnitexToolLogger", "Locate", "-t" + PFX + path + id + "temp.snt", "" + PFX + path + "graph/sanspapier.fst2",
                    "-a" + PFX + path + "common/Alphabet.txt", "-L", "-M", "-n200", "-m" + PFX + path + "dic/auteurs.bin", "-m" + PFX + path + "dic/sample-dlcf.bin",
                    "-m" + PFX + path + "dic/dela-fr-public.bin", "-m" + PFX + path + "dic/stopwords.bin", "-b", "-Y"}) != 0) {
            LOG.error("Error in Second Locatee");
            return "";
        }
        if (UnitexJni.execUnitexTool(new String[]{"UnitexToolLogger", "Concord", "" + PFX + path + id + "temp_snt/concord.ind", "-fCourier 10 Pitch",
                    "-s12", "-l0", "-r0", "-a" + PFX + path + "common/Alphabet_sort.txt", "--CL", "-t"}) != 0) {
            LOG.error("Error in Concord");
            return "";
        }
        String s;

        if ((s = UnitexJni.getUnitexFileString(PFX + path + id + "temp_snt/concord.txt")) == null) {
            LOG.error("Error in getUnitexFileString temp_snt/concord.txt");
            return "";
        }

        return s;
    }
}
