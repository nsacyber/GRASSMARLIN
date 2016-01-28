package prefuse.util.ui;

import java.io.IOException;
import java.net.URL;

/**
 * <p>
 * Browser launcher will open a URL in an external browser on your system.
 *  (e.g. Internet Explorer or Netscape). If your browser is already open,
 *  a new browser window should be created without starting any new processes.
 * </p>
 *
 * <p>
 * On Windows systems the system's default browser will be used. On UNIX and other
 *  platforms the browser defaults to Netscape. For this default behavior to work,
 *  the command 'netscape' must be on your path.
 * </p>
 *
 * <p>
 * This class was inspired by an article at www.javaworld.com by Steven Spencer.
 *  The article is available at
 *  <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">
 *   http://www.javaworld.com/javaworld/javatips/jw-javatip66.html
 *  </a>.
 * </p>
 *
 * @author Steven Spencer
 * @author jeffrey heer (adapted original author's code)
 */
public abstract class BrowserLauncher {

    private static final String WIN_ID = "Windows";
    private static final String WIN_PATH = "rundll32";
    private static final String WIN_FLAG = "url.dll,FileProtocolHandler";
    private static final String UNIX_PATH = "netscape";
    private static final String UNIX_FLAG = "-remote openURL";

    /**
     * Display a file in the system browser.
     * @param url the file's url
     */
    public static void showDocument(URL url) {
        showDocument(url.toString());
    }

    /**
     * Display a file in the system browser.  If you want to display a
     * file, you must include the absolute path name.
     * @param url the file's url (the url must start with either 
     *   "http://" or "file://").
     */
    public static void showDocument(String url) {
        boolean windows = isWindowsPlatform();
        String cmd = null;
        try {
            if (windows) {
                // cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
                cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
                Runtime.getRuntime().exec(cmd);
            } else {
                // Under Unix, Netscape has to be running for the "-remote"
                // command to work.  So, we try sending the command and
                // check for an exit value.  If the exit command is 0,
                // it worked, otherwise we need to start the browser.
                // cmd = 'netscape -remote openURL(http://www.javaworld.com)'
                cmd = UNIX_PATH + " " + UNIX_FLAG + "(" + url + ")";
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    // wait for exit code -- if it's 0, command worked,
                    // otherwise we need to start the browser up.
                    int exitCode = p.waitFor();
                    if (exitCode != 0) {
                        // Command failed, start up the browser
                        // cmd = 'netscape http://www.javaworld.com'
                        cmd = UNIX_PATH + " " + url;
                        p = Runtime.getRuntime().exec(cmd);
                    }
                } catch (InterruptedException x) {
                    System.err.println(
                        "Error bringing up browser, cmd='" + cmd + "'");
                    System.err.println("Caught: " + x);
                }
            }
        } catch (IOException x) {
            // couldn't exec browser
            System.err.println("Could not invoke browser, command=" + cmd);
            System.err.println("Caught: " + x);
        }
    }

    /**
     * Try to determine whether this application is running under Windows
     * or some other platform by examing the "os.name" property.
     * @return true if this application is running under a Windows OS
     */
    public static boolean isWindowsPlatform() {
        String os = System.getProperty("os.name");
        return (os != null && os.startsWith(WIN_ID));
    }

    /**
     * Opens the URL specified on the command line in
     *  the system browser.
     * @param argv argument array. Only the first argument is considered.
     */
    public static void main(String[] argv) {
        showDocument(argv[0]);
    }

} // end of class BrowserLauncher
