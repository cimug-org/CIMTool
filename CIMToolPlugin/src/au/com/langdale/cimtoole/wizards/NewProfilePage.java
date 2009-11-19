/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.Column;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.NSChecker;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.LocalFileBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class NewProfilePage extends FurnishedWizardPage {

    private final String NAMESPACE = Info.getPreference(Info.PROFILE_NAMESPACE);
    private final String ENVELOPE = Info.getPreference(Info.PROFILE_ENVELOPE);

    private IFile file;

    private ProjectBinding projects = new ProjectBinding();
    private LocalFileBinding filename = new LocalFileBinding("owl", true);
    private TextBinding namespace = new TextBinding(Validators.NAMESPACE, NAMESPACE);
    private TextBinding envelope = new TextBinding(Validators.NCNAME, ENVELOPE);
    private NSChecker checker = new NSChecker();

    public NewProfilePage() {
        super("main");
    }

    public void setSelected(IStructuredSelection selection) {
        projects.setSelected(selection);
    }

    public IFile getFile() {
        return file;
    }

    public String getNamespace() {
        return namespace.getText();
    }

    public String getEnvname() {
        return envelope.getText();
    }


    public Boolean getFillProfile() {
        return getContent().getButton("fill").getSelection();
    }


    @Override
    protected Content createContent() {
        return new Content() {


            @Override
            protected Template define() {
                return Column(
                    Grid(
                        Group(Label("Namespace URI:"), Field("namespace")),
                        Group(Label("Project")),
                        Group(CheckboxTableViewer("projects")),
                        Group(Label("Profile name:"), Field("filename")),
                        Group(Label("Envelope Element Name"), Field("envelope"))),
                    CheckBox("fill", "Copy schema contents")
                );
            }

            @Override
            protected void addBindings() {
                projects.bind("projects", this);
                filename.bind("filename", this);
                namespace.bind("namespace", this);
                envelope.bind("envelope", this);
            }

            @Override
            public String validate() {
                IProject project = projects.getProject();
                file = filename.getFile(Info.getProfileFolder(project));

                if( file.exists())
                    return "A profile of that name already exists.";

                return checker.validate(project, namespace.getText(), getContainer(), getShell());
            }
        };
    }
}