package de.tu_berlin.pes.cormorant.ExpressionExtraction.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import de.tu_berlin.pes.memo.MeMoPlugin;

public class Dummy_old implements IWorkbenchWindowActionDelegate{

	@Override
	public void run(IAction action) {
		// TODO Auto-generated method stub
		MeMoPlugin.out.println("Hello World from Dummy");
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		
	}

}
