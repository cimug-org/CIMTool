package au.com.langdale.cimtoole.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

		File destDir = new File(destDirectory);

		// create output directory if it doesn't exist
		if (!destDir.exists())
			destDir.mkdirs();

		// buffer for read and write data to file
		byte[] buffer = new byte[1024];

		ZipInputStream zis = null;

		try {
			zis = new ZipInputStream(is);
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(destDir, fileName);
				
				System.out.println("Unzipping to " + newFile.getAbsolutePath());

				// create directories for sub directories in zip
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				
				fos.close();

				// close this ZipEntry
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// close last ZipEntry
			try {
				zis.closeEntry();
			} catch (IOException e) {
			}
			try {
				zis.close();
			} catch (IOException e) {
			}
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

}
