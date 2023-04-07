# Exchange a Project
**CIMTool** projects can be shared with others.

## Exporting to a Zip File
To send a project to someone else create a zip file as follows:

1. Choose File > Export...

2. Choose General > Archive file and click Next.

3. Check the projects you wish to include in the zip file on the left list.

Be careful not to uncheck any of their contents or sub-folders. The `.project` and `.cimtool-settings` files are required.

4. Select a name and location for the zip file and click Finish.

## Importing from a Zip File
If you have received a zip file that was created from a CIMTool project, you can add it to your current workspace as follows:

1. Choose File > Import...

2. Choose General > Existing Projects into Workspace and click Next. Don't choose the Archive file wizard here.

3. Choose "Select archive file" and click Browse...

4. Locate your zip file. There is no need for it to be unzipped, If it is named `.zipped` instead of `.zip` be sure to adjust the file type filter to `*.*`

5. The projects found in the zip file will now be listed. Check the ones you wish to add to the workspace and click Finish.

Tip: if a project in the zip file has the same name as an existing project in the workspace it cannot be imported. In that case the workspace project should be renamed before running the import wizard. Right click the project in the project explorer and choose Rename...

# Importing from Another Workspace
To copy a project from one workspace to another:

1. Run **CIMTool** in the destination workspace and follow the import procedure.

2. Instead of choosing an archive file select the source workspace directory as the "root directory". The wizard will list all the projects found under this directory.

3. Complete the import procedure as usual.

There is no need to export the projects from the source workspace if it is directly accessible.

!!! note

    It is not a good idea to exchange an entire workspace between different users or hosts. Workspace metadata is not generally portable. Use project import/export instead.