/*Copyright (C) 2013 Mark Ciecior

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.markciecior.snmp.intuptime;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*This class reads config values from a stored file and populates the GUI.
If the stored file isn't available, an example file in the bin/ folder is used instead.
Once the GO! button is hit, the values from the GUI are stored to the user's home directory.
*/
public class Config {

    private static final Properties properties = new Properties();
    private static FileReader fr;
    private static InputStream is;
    private static String CONFIG_FILENAME = "com.markciecior.snmp.intuptime.config.properties";

    static {
    	//Look for the stored values in the user's home first
        try {
        	fr = new FileReader(System.getProperty("user.home") + "/" + CONFIG_FILENAME);
        } catch (FileNotFoundException e) {
        	fr = null;
        }
        //Try the barebones file in the bin/ folder too
        try {
        	is = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME);
        } catch (NullPointerException e) {
           	is = null;
        }
        //Look for the stored values in the user's home first
        if (fr != null) {
        	try {
        		properties.load(fr);
        	} catch (IOException e) {
        	}
        //If not in the home dir, grab the barebones file from the bin/ folder
        } else if (is != null) {
        	try {
        		properties.load(is);
        	} catch (IOException e) {
        		
        	}
        }
        
    }

    public static String getSetting(String key) {
        return properties.getProperty(key);
    }
    
    /*When the GO! button is hit, store the values to the home directory
    */
    public static void setSetting(String key, String value){
    	properties.setProperty(key, value);
    	FileWriter fw;
		try {
			fw = new FileWriter(System.getProperty("user.home") + "/" + CONFIG_FILENAME);
			properties.store(fw, null);
		} catch (IOException e) {
			e.printStackTrace();
		} 
    	
    }

    // ...
}