package au.com.langdale.cimtoole.pandoc;
import java.io.File;
import java.io.IOException;

public class PandocPathResolver {

	private static final String OS_NAME = "os.name";
	
	private static File embeddedPandocRootDir = null;
	private static File externalPandocRootDir = null;
	
	public static void setEmbeddedPandocRootDir(File rootDir) {
		embeddedPandocRootDir = rootDir;
	}
	
	public static void setExternalPandocRootDir(File rootDir) {
		externalPandocRootDir = rootDir;
	}
	
    /**
     *  Detects the OS and returns the appropriate Pandoc executable path.
     *  
     * @return The Pandoc executable path resolved based on OS and Pandoc installation factors.
     * @throws IOException
     */
    public static File getPandocExecutablePath() throws IOException {
    	File rootDir = (externalPandocRootDir != null ? externalPandocRootDir : embeddedPandocRootDir);

    	String os = System.getProperty(OS_NAME).toLowerCase();
    	if (os.contains("win")) {
    		// Windows: pandoc.exe at the root of the extracted archive.
    		return new File(rootDir, "pandoc.exe");
    	} else {
    		// macOS and Linux: bin/pandoc within the extracted archive.
    		return new File(new File(rootDir, "bin"), "pandoc");
    	}
    }
}
