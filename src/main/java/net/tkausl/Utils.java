/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tkausl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 *
 * @author Tobias
 */
public class Utils {
    
    public static void patchJar(File inJar, File outJar, File patchJar) throws IOException{
        JarInputStream inStream = null;
        JarFile patchFile = null;
        JarOutputStream outStream = null;
        try {
            inStream = new JarInputStream(new FileInputStream(inJar));
            outStream = new JarOutputStream(new FileOutputStream(outJar), inStream.getManifest());
            patchFile = new JarFile(patchJar);
            JarEntry entry;
            JarEntry patchEntry;
            int len;
            byte[] buf = new byte[2048];
            InputStream is;
            while((entry = inStream.getNextJarEntry()) != null){
                patchEntry = patchFile.getJarEntry(entry.getName());
                if(patchEntry != null){
                    entry = patchEntry;
                    is = patchFile.getInputStream(patchEntry);
                } else {
                    is = inStream;
                }
                outStream.putNextEntry(entry);
                while((len = is.read(buf)) >= 0){
                    outStream.write(buf, 0, len);
                }
            }
        } finally {
            safeClose(inStream);
            safeClose(outStream);
            safeClose(patchFile);
        }
    }
    
    public static void extractResource(String resource, File to) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = Utils.class.getResourceAsStream(resource);
            if(is == null) throw new IOException("Resource '" + resource + "' not found.");
            os = new FileOutputStream(to);
            int len;
            byte[] buf = new byte[2048];
            while((len = is.read(buf)) >= 0){
                os.write(buf, 0, len);
            }
        } finally {
            safeClose(is);
            safeClose(os);
        }
    }
    
    public static void safeClose(InputStream is){
        try {
            if(is != null) is.close();
        }catch(Throwable t){}
    }
    
    public static void safeClose(OutputStream os){
        try {
            if(os != null) os.close();
        }catch(Throwable t){}
    }
    
        public static void safeClose(JarFile os){
        try {
            if(os != null) os.close();
        }catch(Throwable t){}
    }
}
