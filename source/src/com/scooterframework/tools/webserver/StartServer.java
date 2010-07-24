/*
 *   This software is distributed under the terms of the FSF
 *   Gnu Lesser General Public License (see lgpl.txt).
 *
 *   This program is distributed WITHOUT ANY WARRANTY. See the
 *   GNU General Public License for more details.
 */
package com.scooterframework.tools.webserver;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * StartServer class starts web server. This class exposes the following 
 * system properties if they have not been defined. 
 * <pre>
 *     scooter.home -> path to scooter home
 *     app.name     -> application name
 *     app.logs     -> location of application logs
 *     app.path     -> path to a specific web app
 *     jetty.home   -> path to jetty server
 *     jetty.logs   -> location of jetty logs
 *     jetty.port   -> port number of jetty server
 *     jdk.home     -> path to JDK (only if tools.jar is detected)
 *     START        -> configuration file of jetty server
 *     webapps.name -> name of webapps directory (default is webapps) 
 *     webapps.path -> path to webapps directory
 * </pre>
 * 
 * Original Jetty command line options are also supported: 
 * <tt>-DDEBUG, --help, --version, --stop</tt>
 * 
 * <p>
 * Examples:
 * <pre>
    Usage:    
        java -jar tools/server.jar app_name [port, [config ...]]    
        
    Examples:    
        This page:    
            java -jar tools/server.jar -help    
        
        Run Jetty Server with default jetty.xml in tools/servers/jetty/etc and port 8080:    
            java -jar tools/server.jar blog    
        
        Run Jetty Server with default jetty.xml in tools/servers/jetty/etc but on port 8090:    
            java -jar tools/server.jar blog 8090    
        
        Run Jetty Server with the blog sample app on port 8091:    
            java -jar tools/server.jar examples/blog 8091    
        
        Run Jetty Server with the blog sample app installed in user home:    
            java -jar tools/server.jar /home/john/blog   
        
        Run Jetty Server as JettyPlus (JNDI, JAAS etc.) with config files in tools/servers/jetty/etc:    
            java -jar tools/server.jar blog etc/jetty-plus.xml etc/jetty.xml 
 * </pre>
 * </p>
 *
 * @author (Fei) John Chen
 */
public class StartServer {

	public static void main(String[] args) {
    	if ((args.length > 0 && args[0].equalsIgnoreCase("-help")) || args.length < 1) {
			usage();
			return;
		}

		try {
			doTheWork(args);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			log("ERROR ERROR ERROR: " + ex.getMessage());
		}
    }

	private static void doTheWork(String[] args) throws Exception {
    	String scooterHome = (new File("")).getCanonicalPath();
    	scooterHome = setSystemProperty("scooter.home", scooterHome);

        String jettyHome = scooterHome + File.separator + "tools" + 
        				File.separator + "servers" + File.separator + "jetty";
        jettyHome = setSystemProperty("jetty.home", jettyHome);

		String START = jettyHome + File.separator + "start.config";
		START = setSystemProperty("START", START);
		
		if (args[0].startsWith("--")) {
			startUp(args);
			return;
		}
		
    	String webappsName = setSystemProperty("webapps.name", "webapps");
		
		String appName = "";
		String webappsPath = "";
		String webappPath = "";
		String firstArg = args[0];
		if (containsPath(firstArg)) {
			String[] ss = getPathAndName(firstArg);
			webappsPath = ss[0];
			webappPath  = ss[1];
			appName     = ss[2];
		}
		else {
			appName = firstArg;
	        webappsPath = scooterHome + File.separator + webappsName;
	        webappPath = webappsPath + File.separator + appName;
		}

        System.setProperty("app.name", appName);
        webappsPath = setSystemProperty("webapps.path", webappsPath);
        webappPath = setSystemProperty("app.path", webappPath);
		
        validateWebappExistence(webappPath);
        
    	boolean foundPort = false;
    	List jettyXMLs = new ArrayList();

    	String port = "8080";
    	String jettyXML = "etc/jetty.xml";
    	if (args.length > 1) {
    		foundPort = isNumber(args[1]);
    		if (foundPort) {
    			port = args[1];
    			System.setProperty("jetty.port", port);
    			
    			if (args.length == 2) {
    				jettyXMLs.add(jettyXML);
    			}
    			else {
    				jettyXMLs.addAll(getXMLs(args, 2));
    			}
    		}
    		else {
    			jettyXMLs.addAll(getXMLs(args, 1));
    		}
        }
    	else if (args.length == 1) {
    		jettyXMLs.add(jettyXML);
    	}
        
        String jettyLogs = jettyHome + File.separator + "logs";
        jettyLogs = setSystemProperty("jetty.logs", jettyLogs);
    	String appLogs = webappPath + File.separator + "WEB-INF" + 
    								  File.separator + "log";
    	appLogs = setSystemProperty("app.logs", appLogs);
    	
    	validateXMLsExistence(jettyHome, jettyXMLs);
        String jettyXMLsAsString = getXMLsAsString(jettyHome, jettyXMLs);
        
		String jdkHome = getJDKHome();
		boolean foundToolsJar = existsToolsJar(jdkHome);
		if (foundToolsJar) {
			jdkHome = setSystemProperty("jdk.home", jdkHome);
		}

        String hr = "=========================================";

        log(hr);
        log("");
        if (foundPort) {
        	log("Starting Jetty Web Server on port " + port);
        }
        else {
        	log("Starting Jetty Web Server");
        }

        //some basic info
        log("");
        log("scooter.home: " + scooterHome);
        log("    app.name: " + appName);
        log("    app.logs: " + appLogs);
        log("    app.path: " + webappPath);
        log("  jetty.home: " + jettyHome);
        log("  jetty.logs: " + jettyLogs);
        log("  jetty.init: " + START);
        log("  jetty.xmls: " + jettyXMLsAsString);
        log("");

        if (!foundToolsJar) {
        	log("No tools.jar found in \"" + jdkHome + File.separator + "lib\". ");
        	log("tools.jar is required if using DEVELOPMENT mode.");
        	log("");
        }
        log("Use Ctrl-C to shutdown server");
        log("");
        log(hr);
        startUp(jettyHome, jettyXMLs);
    }
	
	private static boolean containsPath(String s) {
		boolean check = false;
		if (s.indexOf(File.separatorChar) != -1 || s.indexOf('/') != -1) {
			check = true;
		}
		return check;
	}
	
	private static String[] getPathAndName(String s) {
		String[] ss = new String[3];
		File f = new File(s);
		try {
			String filePath = f.getCanonicalPath();
			String parentPath = (new File(filePath)).getParentFile().getCanonicalPath();
			ss[0] = parentPath;
			ss[1] = filePath;
			ss[2] = (parentPath.endsWith(File.separator))?
					filePath.substring(parentPath.length()):
					filePath.substring(parentPath.length() + 1);
		}
		catch(Exception ex) {
			;
		}
		return ss;
	}
	
	private static String setSystemProperty(String key, String defaultValue) {
		String p = System.getProperty(key);
		if (p == null) {
			System.setProperty(key, defaultValue);
			p = defaultValue;
		}
		return p;
	}

	private static boolean isNumber(String s) {
		boolean status = true;
		try {
			Integer.parseInt(s);
		}
		catch(Exception ex) {
			status = false;
		}
		return status;
	}

	private static List getXMLs(String[] ary, int start) {
		List list = new ArrayList();
		for (int i = start; i < ary.length; i++) {
			list.add(ary[i]);
		}
		return list;
	}

	private static void validateWebappExistence(String webappPath) {
		File f = new File(webappPath);
        if (!f.exists()) {
            log("");
            log("ERROR ERROR ERROR: Web application [" +
            		webappPath + "] does not exist.");
            log("");
            log("System exits now.");
            System.exit(-1);
        }
	}

	private static void validateXMLsExistence(String jettyHome, List names) {
		Iterator it = names.iterator();
		while(it.hasNext()) {
			String name = (String)it.next();
			String jettyXMLPath = jettyHome + File.separator + name;
			File jx = new File(jettyXMLPath);
            if (!jx.exists()) {
                log("");
                log("ERROR ERROR ERROR: The required jetty configuration file [" +
                		jettyXMLPath + "] does not exist.");
                log("");
                log("System exits now.");
                System.exit(-1);
            }
		}
	}

	private static String getXMLsAsString(String jettyHome, List jettyXMLs) {
		StringBuffer sb = new StringBuffer();
		Iterator it = jettyXMLs.iterator();
		while(it.hasNext()) {
			sb.append(jettyHome).append(File.separator);
			sb.append(it.next()).append(", ");
		}
		String s = sb.toString();
		return (s.endsWith(", "))?s.substring(0, s.length()-2):s;
	}

	private static String getJDKHome() {
		String s = System.getProperty("java.home");
		return s.substring(0, s.length()-4);
	}

	private static boolean existsToolsJar(String jdkHome) {
		boolean exist = false;
		File f = new File(jdkHome + File.separator +
				"lib" + File.separator + "tools.jar");
        if (f.exists()) {
        	exist = true;
        }

		return exist;
	}

    public static void startUp(String[] args)
    throws Exception
    {
    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
    	String jettyMainClassName = "org.mortbay.start.Main";
    	invokeMain(cl, jettyMainClassName, args);
    }

    public static void startUp(String jettyHome, List jettyXMLs)
    throws Exception
    {
    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
    	String jettyMainClassName = "org.mortbay.start.Main";
    	String[] args=(String[])jettyXMLs.toArray(new String[jettyXMLs.size()]);
    	for (int i = 0; i < jettyXMLs.size(); i++) {
    		args[i] = jettyHome + File.separator + args[i];
    	}

    	invokeMain(cl, jettyMainClassName, args);
    }

    private static void invokeMain(ClassLoader classloader, String classname, String[] args)
    throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, ClassNotFoundException
    {
        Class invoked_class = classloader.loadClass(classname);
        Class[] method_param_types = new Class[1];
        method_param_types[0]= args.getClass();
        Method main = invoked_class.getDeclaredMethod("main", method_param_types);
        Object[] method_params = new Object[1];
        method_params[0] = args;

        main.invoke(null, method_params);
    }

	private static void log(Object o) {
    	System.out.println("scooTer> " + o);
    }

    private static void usage() {
    	log("Summary:");
    	log("    This is a utility that starts an embedded Jetty Web Server.");
    	log("");
    	log("Usage:");
    	log("    java -jar tools/server.jar app_name [port, [config ...]]");
    	log("");
    	log("Examples:");
    	log("    This page:");
    	log("        java -jar tools/server.jar -help");
    	log("");
    	log("    Run Jetty Server with default jetty.xml in tools/servers/jetty/etc and port 8080:");
    	log("        java -jar tools/server.jar blog");
    	log("");
    	log("    Run Jetty Server with default jetty.xml in tools/servers/jetty/etc but on port 8090:");
    	log("        java -jar tools/server.jar blog 8090");
    	log("");
    	log("    Run Jetty Server with the blog sample app on port 8091:");
    	log("        java -jar tools/server.jar examples/blog 8091");
    	log("");
    	log("    Run Jetty Server with the blog sample app installed in user home:");
    	log("        java -jar tools/server.jar /home/john/blog");
    	log("");
    	log("    Run Jetty Server as JettyPlus (JNDI, JAAS etc.) with config files in tools/servers/jetty/etc:");
    	log("        java -jar tools/server.jar blog etc/jetty-plus.xml etc/jetty.xml");
    	log("");
    }
}