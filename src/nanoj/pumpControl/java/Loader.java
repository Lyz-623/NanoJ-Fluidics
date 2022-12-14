package nanoj.pumpControl.java;

import org.apache.commons.lang3.SystemUtils;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * A utility to add classes to the classpath. It loads either all Jars or native libraries in a given directory.
 */
public class Loader {
    static final ArrayList<File> NO_FILE_FOUND = new ArrayList<File>();
    static final String DLL = ".dll";
    static final String JNILIB = ".jnilib";
    static final String JAR = ".jar";

    public static void loadJars(URL url) throws IntrospectionException, MalformedURLException, FileNotFoundException {
        // Adapted from http://baptiste-wicht.com/posts/2010/05/tip-add-resources-dynamically-to-a-classloader.html
        URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

        ArrayList<File> listOfFiles = findFilesOfType(url, JAR);

        if (listOfFiles.isEmpty())
            throw new FileNotFoundException("Did not find any jar files in " + url.getPath());

        for (File currentFile: listOfFiles) {
            URL currentURL = currentFile.toURI().toURL();

            try {
                Method method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(systemClassLoader, currentURL);
                System.out.println("Loaded the jar: " + currentFile.getName());
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IntrospectionException("Error when adding url to system ClassLoader.");
            }
        }
    }

    public static void loadLibrary(URL url) throws FileNotFoundException, MalformedURLException {
        String lib;

        if (SystemUtils.IS_OS_WINDOWS) lib = DLL; //dll=Dynamic Link Library
        else lib = JNILIB;

        ArrayList<File> listOfFiles = findFilesOfType(url, lib);
        if (listOfFiles.isEmpty())
            throw new FileNotFoundException("Did not find library files in " + url.getPath());

        //need VC++2010(or below) to successfully load some dll files
        for (File currentFile : listOfFiles) {
            try {
                System.load(currentFile.getPath());
                System.out.println("Loaded the library: " + currentFile.getName());
            } catch(UnsatisfiedLinkError e){
                System.err.println("Native code library failed to load.\n" + e);
                System.exit(1);
            }
        }
    }
    //test

    public static ArrayList<File> findFilesOfType(URL url, String termination) throws MalformedURLException{
        File folder = new File(url.getPath());
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) return NO_FILE_FOUND;

        ArrayList<File> filteredListOfFiles = new ArrayList<File>();

        for (File currentFile: listOfFiles) {
            String currentPath = currentFile.getAbsolutePath();

            if (currentPath.substring(currentPath.length()-termination.length()).equals(termination)) {
                filteredListOfFiles.add(currentFile);
            }
        }

        return filteredListOfFiles;
    }
}