package au.com.langdale.cimtoole.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class WorkspaceTest extends TestUtility {

	protected IProject project;
	protected IProgressMonitor monitor;
	protected IWorkspace workspace;

	@Override
	protected void setUp() throws Exception {
		workspace = ResourcesPlugin.getWorkspace();
		monitor = new NullProgressMonitor();
		project = workspace.getRoot().getProject("TestProject");
		if (project.exists())
			project.delete(true, monitor);
	}

	@Override
	protected void tearDown() throws Exception {
//		if( project.exists())
//			project.delete(true, monitor);
	}

	protected void unzip(InputStream is, String destDirectory) {
		Path destDir = Paths.get(destDirectory);

		try (ZipInputStream zis = new ZipInputStream(is)) {
			
			// Create destination directory if it doesn't exist
			if (!Files.exists(destDir)) {
				Files.createDirectories(destDir);
			}
			
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				Path entryPath = destDir.resolve(entry.getName());

				// Security check: prevent zip slip vulnerability
				if (!entryPath.normalize().startsWith(destDir.normalize())) {
					throw new IOException("Bad zip entry: " + entry.getName());
				}

				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					// Create parent directories if needed
					Files.createDirectories(entryPath.getParent());
					// Extract file - this is the key method!
					Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
				}
				zis.closeEntry();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
