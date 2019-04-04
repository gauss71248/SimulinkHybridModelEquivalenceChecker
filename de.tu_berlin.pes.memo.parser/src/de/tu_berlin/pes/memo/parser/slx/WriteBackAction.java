package de.tu_berlin.pes.memo.parser.slx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.parser.persistence.MeMoPersistenceManager;
import de.tu_berlin.pes.memo.parser.slx.opc.OPCWriter;
import de.tu_berlin.pes.memo.project.ProjectNature;

public class WriteBackAction implements IActionDelegate {

	private IFile selectedFile = null;

	@Override
	public void run(IAction action) {

		try {
			MeMoPersistenceManager persistence = MeMoPersistenceManager.getInstance();
			IProject project = selectedFile.getProject();
			if (project.getPersistentProperty(ProjectNature.ACTIVEDATABASE) == null) {
				MeMoPlugin.out.println(project.getName() + " is not parsed yet");
				MeMoPlugin.logException(project.getName() + " is not parsed yet", new IllegalArgumentException());
				return;
			}
			MeMoPlugin.out.println("Loading Model "+selectedFile.getProject().getName()+"...");
			if (!project.getPersistentProperty(ProjectNature.ACTIVEDATABASE).equals(persistence.getDatabaseName())) {
				persistence.switchDatabase(project.getPersistentProperty(ProjectNature.ACTIVEDATABASE));
			}
			Model m = persistence.getModel();
			IFolder output = selectedFile.getProject().getFolder("output"+File.separator+"writeBack");
			if (!output.exists()) {
				try {
					output.create(IResource.NONE, true, null);
				} catch (CoreException e) {
					MeMoPlugin.logException("", e);				}
			}
			SLXWriter w = new SLXWriter(m, new File(output.getLocation().toOSString()+File.separator +selectedFile.getName().replaceAll("."+selectedFile.getFileExtension(), "")+"_writeback"+"."+selectedFile.getFileExtension()));
			w.writeModel();
			output.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection.isEmpty()) {
			return;
		}

		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj instanceof IFile) {
				selectedFile  = (IFile) obj;
			}

			return;
		}
	}

}
