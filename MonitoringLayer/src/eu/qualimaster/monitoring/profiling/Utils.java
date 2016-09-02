/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.monitoring.profiling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Some utilities.
 * 
 * @author Holger Eichelberger
 * @author Christopher Voges
 */
public class Utils {
    
    /**
     * Stores the content to the given file.
     * 
     * @param file {@link File} to write to.
     * @param content The lines to write.
     * @param key the identification key for this profile
     * @throws IOException in case of I/O problems 
     */
    public static void writeTxtFile(File file, ArrayList<String> content, String key) throws IOException {
        PrintStream writer = null;
        try {
            writer = new PrintStream(file);
            for (String out : content) {
                writer.println(out);
            }
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }
    
    /**
     * Loads the content of the given file into the returned {@link ArrayList}.
     * 
     * @param file {@link File} to load.
     * @param key the identification key for this profile
     * @return the content
     * @throws IOException in case of I/O problems 
     */
    public static ArrayList<String> loadTxtFile(File file, String key) throws IOException {
        ArrayList<String> content = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // No comments or empty lines
                if (!(line.startsWith("#")) && !(line.isEmpty())) {
                    content.add(line);
                }
            }
        } finally {
            if (null != reader) {
                reader.close();
            }
        }
        return content;
    }
    
    /**
     * Loads a properties file from <code>file</code>.
     * 
     * @param file the file to load
     * @param data the data read
     * @throws IOException in case of I/O problems
     */
    public static void load(File file, Properties data) throws IOException {
        if (file.exists()) {
            FileReader fr = null;
            try {
                fr = new FileReader(file);
                data.load(fr);
            } finally {
                if (null != fr) {
                    fr.close();
                }
            }
        }
    }
    
    /**
     * Stores a properties file to <code>file</code>.
     * 
     * @param file the file to read
     * @param data the properties to read into
     * @throws IOException in case of I/O problems
     */
    public static void store(File file, Properties data) throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            data.store(fw, "");
        } finally {
            if (null != fw) {
                fw.close();
            }
        }
    }
    
    /**
     * Reads a double from a properties file.
     * 
     * @param prop the properties file
     * @param key the key to read from
     * @param deflt a default value in case that the double value cannot be read
     * @return the value of <code>key</code> as double or <code>deflt</code>
     */
    public static double getDouble(Properties prop, String key, double deflt) {
        double result = deflt;
        try {
            String tmp = prop.getProperty(key);
            if (null != tmp) {
                result = Double.parseDouble(tmp);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    /**
     * Reads a long from a properties file.
     * 
     * @param prop the properties file
     * @param key the key to read from
     * @param deflt a default value in case that the long value cannot be read
     * @return the value of <code>key</code> as long or <code>deflt</code>
     */
    public static long getLong(Properties prop, String key, long deflt) {
        long result = deflt;
        try {
            String tmp = prop.getProperty(key);
            if (null != tmp) {
                result = Long.parseLong(tmp);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

    /**
     * Reads an int from a properties file.
     * 
     * @param prop the properties file
     * @param key the key to read from
     * @param deflt a default value in case that the int value cannot be read
     * @return the value of <code>key</code> as int or <code>deflt</code>
     */
    public static int getInt(Properties prop, String key, int deflt) {
        int result = deflt;
        try {
            String tmp = prop.getProperty(key);
            if (null != tmp) {
                result = Integer.parseInt(tmp);
            }
        } catch (NumberFormatException e) {
        }
        return result;
    }

}
