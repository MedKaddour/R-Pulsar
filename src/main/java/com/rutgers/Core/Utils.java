/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Core;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
* This is used to implement miscellaneous tools need it in R-Pulsar.
* 
* @author  Eduard Giber Renart
* @version 1.0
*/
public class Utils {
    
	/** 
	 * Method used to execute linux commands.
	 * @param command Linux command to execute.
	 * @return
	 */
    public static String executeCommand(String command) {
        StringBuffer output = new StringBuffer();        
        System.out.println("Command: " + command);
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                    output.append(line + "\n");
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
        return output.toString();
    }
    
    /**
     * Convert a long to a byte array.
     * @param value
     * @return
     */
    public static byte[] longToByteArray(long value) {
        return new byte[] {
            (byte) (value >> 56),
            (byte) (value >> 48),
            (byte) (value >> 40),
            (byte) (value >> 32),
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }
    
    /**
     * Get the bigger number given two numbers.
     * @param first
     * @param second
     * @return
     */
    public static int getMax(int first, int second) {
        return first > second ? first : second;
    }
    
    /**
     * Write a string to a File.
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] writeFileToBytes(String file) throws IOException {
        Path path = Paths.get(file);
        return Files.readAllBytes(path);
    }
    
    public static void writeBytesToFile(byte[] bFile, String fileDest) {
        try (FileOutputStream fileOuputStream = new FileOutputStream(fileDest)) {
            fileOuputStream.write(bFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
