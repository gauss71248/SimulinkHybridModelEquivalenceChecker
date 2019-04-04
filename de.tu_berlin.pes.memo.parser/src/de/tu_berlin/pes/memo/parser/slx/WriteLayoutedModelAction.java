package de.tu_berlin.pes.memo.parser.slx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.graph_embedding.Executor;
import de.tu_berlin.pes.memo.graph_embedding.ExecutorOptions;
import de.tu_berlin.pes.memo.graph_embedding.PhaseExecutor.Phase1;
import de.tu_berlin.pes.memo.graph_embedding.PhaseExecutor.Phase2;
import de.tu_berlin.pes.memo.graph_embedding.PhaseExecutor.Phase3;
import de.tu_berlin.pes.memo.graph_embedding.PhaseExecutor.Phase4;
import de.tu_berlin.pes.memo.graph_embedding.PhaseExecutor.Phase5;
import de.tu_berlin.pes.memo.graph_embedding.common.LayeredGraphTransformer;
import de.tu_berlin.pes.memo.graph_embedding.config.GraphConfig;
import de.tu_berlin.pes.memo.graph_embedding.model.Graph;
import de.tu_berlin.pes.memo.graph_embedding.model.Layer;
import de.tu_berlin.pes.memo.graph_embedding.model.Node;
import de.tu_berlin.pes.memo.graph_embedding.model.edges.Edge;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.parser.persistence.MeMoPersistenceManager;
import de.tu_berlin.pes.memo.project.ProjectNature;

public class WriteLayoutedModelAction implements IActionDelegate {
	
	private IFile selectedFile = null;
	
	private static String libraryDir = null;
	
	private static String libraryDirectory() {
		if (libraryDir == null) {
			URL url = Platform.getBundle("de.tu_berlin.pes.memo.layouting").getEntry("lib/f");
			String fileURL = null;
			try {
				fileURL = FileLocator.toFileURL(url).toString();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			libraryDir = fileURL.substring(6, fileURL.length() - 1);
		}
		return libraryDir;
	}
	
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
			// TODO
			
			LayeredGraphTransformer lgt = new LayeredGraphTransformer();
			GraphConfig gc = new GraphConfig();
			Graph g = lgt.transform(m, gc);
			g.eo = new ExecutorOptions(Phase1.HEURISTIC_BETTER, Phase2.ILP, Phase3.Barycenter_PartialOrdering,  Phase4.ILP, Phase5.Heuristic_Sandner, 1.0, 4.0, 0.01, false, g.gc);
			Executor.layoutGraph(g, libraryDirectory());
			
			HashMap<Block,Block> blockMapping = new HashMap<>();
			
			for(Layer l : g.layers) {
				int x = l.x;
				for(Node n : l.nodes) {
					int y = n.y;
					if(n.origin instanceof Block) {
						Block newBlock = (Block) n.origin;
						Block oldBlock = m.getBlockByPath(newBlock.getFullQualifiedName(false),false);
						int top = y;
						int left = x;
						int bottom = top+n.height;
						int right = left+n.width;
						newBlock.getParameter().put("Position", "["+left+" "+top+" "+right+" "+bottom+"]");
						blockMapping.put(oldBlock, newBlock);
					}
				}
			}
			
			HashMap<SignalLine,SignalLine> signalMapping = new HashMap<>();
			for(Edge e : g.edges) {
				if(e.origin instanceof SignalLine) {
					SignalLine newLine = (SignalLine) e.origin;
					SignalLine oldLine = m.getSignalMap().get(newLine.getId());
					assert newLine.getName().equals(oldLine.getName());
					signalMapping.put(oldLine, newLine);
				}
			}
			
			System.out.println(blockMapping);
			System.out.println(signalMapping);
			
			File inputFile = selectedFile.getLocation().toFile();
			File outputFile = new File(selectedFile.getLocation().toFile().getAbsolutePath()+"_layouted."+selectedFile.getFileExtension());
			
			SlxLayoutTransformer transform = new SlxLayoutTransformer(m,inputFile,outputFile);
			
			System.out.println(transform.blocks());
			transform.transform(blockMapping, signalMapping);
			
			output.refreshLocal(IResource.DEPTH_INFINITE, null);
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
