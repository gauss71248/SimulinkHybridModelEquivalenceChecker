// COPYRIGHT NOTICE (NOT TO BE REMOVED):
//
// This file, or parts of it, or modified versions of it, may not be copied,
// reproduced or transmitted in any form, including reprinting, translation,
// photocopying or microfilming, or by any means, electronic, mechanical or
// otherwise, or stored in a retrieval system, or used for any purpose, without
// the prior written permission of all Owners unless it is explicitly marked as
// having Classification `Public'.
//   Classification: Restricted.
//
// Owners of this file give notice:
//   (c) Copyright 2010-2012 PES Software Engineering for Embedded Systems, TU Berlin
//
// Authors:
//		Sabine Glesner
//		Robert Reicherdt
//		Elke Salecker
//		Volker Seeker
//		Joachim Kuhnert
// 		Roman Busse
//
// All rights, including copyrights, reserved.
//
// This file contains or may contain restricted information and is UNPUBLISHED
// PROPRIETARY SOURCE CODE OF THE Owners.  The Copyright Notice(s) above do not
// evidence any actual or intended publication of such source code.  This file
// is additionally subject to the conditions listed in the RESTRICTIONS file
// and is with NO WARRANTY.
//
// END OF COPYRIGHT NOTICE

package de.tu_berlin.pes.memo.parser.persistence.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.parser.persistence.MeMoPersistenceManager;
import de.tu_berlin.pes.memo.project.ProjectNature;

/**
 * Shows the databases for the currently selected project, updates the
 * MeMoPersistenceManager to the currently relevant database. Enables the user
 * to delete databases and select a database from a list of databases dedicated
 * to the currently selected project.
 *
 * @author Robert Reicherdt, Joachim Kuhnert
 *
 */
public class ActiveDBView extends ViewPart implements ISelectionListener {

	private static ArrayList<ActiveDBView> views = new ArrayList<ActiveDBView>();

	TableViewer viewer;
	private IProject currentProject;

	public class TableEntry {
		boolean active = false;
		String dbName = null;

		public TableEntry(boolean active, String dbName) {
			super();
			this.active = active;
			this.dbName = dbName;
		}

		// @Override
		// public boolean equals(Object o) {
		// if (o instanceof TableEntry) {
		// TableEntry second = (TableEntry) o;
		// return dbName.equals(second.dbName);
		// }
		// return false;
		// }
	}

	public ActiveDBView() {
		views.add(this);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void createPartControl(final Composite parent) {
		// Create viewer.
		viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
		col.getColumn().setText("Active");
		col.getColumn().setWidth(50);
		col = new TableViewerColumn(viewer, SWT.NONE);
		col.getColumn().setText("Database");
		col.getColumn().setWidth(200);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		viewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// NOP

			}

			@Override
			public void dispose() {
				// NOP

			}

			@Override
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}
		});
		viewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				super.update(cell);
				TableEntry te = (TableEntry) cell.getElement();
				if (cell.getColumnIndex() == 0) {
					if (te.active) {
						cell.setImage(getTitleImage());
					}
				} else if (cell.getColumnIndex() == 1) {
					cell.setText(te.dbName);
				}

				if (te.active) {
					cell.setForeground(new Color(parent.getDisplay(), new RGB(0, 170, 0)));
				}
			}
		});

		getViewSite().getPage().addSelectionListener("de.tu_berlin.pes.memo.project.navigator", this);

		viewer.getControl().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				// NOP

			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					deleteSelectedEntry();
				}
			}

		});

		addContexMenu();

		viewer.getControl().addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				// NOP

			}

			@Override
			public void mouseDown(MouseEvent e) {
				// NOP

			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setSelectedEntryActive();

			}
		});

	}

	/**
	 * Creates the context menu so the user can delete and set a database active.
	 */
	private void addContexMenu() {

		final MenuManager mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);

		mgr.addMenuListener(new IMenuListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse
			 * .jface.action.IMenuManager)
			 */
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (!selection.isEmpty()) {

					Action setActive = new Action("Set "
							+ ((TableEntry) selection.getFirstElement()).dbName + " active") {

						@Override
						public void run() {
							setSelectedEntryActive();
						}
					};
					mgr.add(setActive);

					Action del = new Action("Delete "
							+ ((TableEntry) selection.getFirstElement()).dbName) {

						@Override
						public void run() {
							deleteSelectedEntry();
						}
					};
					mgr.add(del);
				}
			}
		});
		viewer.getControl().setMenu(mgr.createContextMenu(viewer.getControl()));
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (first instanceof IResource) {
				IResource res = (IResource) first;
				IProject project = res.getProject();
				currentProject = project;
				try {
					ArrayList<TableEntry> validEntries = getTableEntries();
					viewer.setInput(validEntries.toArray());
					viewer.refresh();

				} catch (CoreException e) {
					MeMoPlugin.logException(e.toString(), e);
				}
			}

		}
	}

	/**
	 * Reads the project properties and generates from them the list of dedicated
	 * databases.
	 *
	 * @return The list of valid (really existing) databases.
	 * @throws CoreException
	 */
	protected ArrayList<TableEntry> getTableEntries() throws CoreException {

		List<String> databases = MeMoPersistenceManager.getInstance().getDatabases();

		ArrayList<TableEntry> validEntries = new ArrayList<TableEntry>();
		if (currentProject.isOpen()) {

			String selected = currentProject.getPersistentProperty(ProjectNature.ACTIVEDATABASE);
			String all = currentProject.getPersistentProperty(ProjectNature.DATABASES);
			if (selected != null) {
				MeMoPersistenceManager.getInstance().switchDatabase(selected);
			}
			if (all != null) {
				String[] content = all.split(File.pathSeparator);
				for (String s : content) {
					if ((s != null) && !s.isEmpty() && databases.contains(s)) {
						validEntries.add(new TableEntry(s.equals(selected), s));
					}

				}
			}
		}
		return validEntries;
	}

	/**
	 * Updates the ActiveDBView if e.g. a new project is selected.
	 */
	public static void update() {
		for (final ActiveDBView dbView : views) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					try {
						dbView.viewer.setInput(dbView.getTableEntries().toArray());
					} catch (CoreException e) {
						MeMoPlugin.logException(e.toString(), e);
					}
				}
			});
		}
	}

	/**
	 * delete an entry and the database.
	 */
	private void deleteSelectedEntry() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		TableEntry selected = (TableEntry) selection.getFirstElement();

		if (selected != null) {
			try {

				MeMoPersistenceManager persistence = MeMoPersistenceManager.getInstance();
				if (persistence.dropDatabase(selected.dbName)) {
					ArrayList<TableEntry> newEntries = new ArrayList<TableEntry>();
					String allString = "";

					for (TableEntry entry : getTableEntries()) {
						if (!entry.dbName.equals(selected.dbName)) {
							newEntries.add(entry);
							allString += entry.dbName + File.pathSeparator;
						}
					}

					currentProject.setPersistentProperty(ProjectNature.DATABASES, allString);

					if (selected.active) {
						currentProject.getProject().setPersistentProperty(ProjectNature.ACTIVEDATABASE,
								null);
					}

					viewer.setInput(newEntries.toArray());
					viewer.refresh();
				}
			} catch (CoreException e1) {
				MeMoPlugin.logException(e1.toString(), e1);
			}
		}
	}

	/**
	 * Set an other database active
	 */
	private void setSelectedEntryActive() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		TableEntry selected = (TableEntry) selection.getFirstElement();

		if (selected != null) {
			try {
				currentProject.getProject().setPersistentProperty(ProjectNature.ACTIVEDATABASE,
						selected.dbName);
				update();
			} catch (CoreException e1) {
				MeMoPlugin.logException(e1.toString(), e1);
			}
		}

	}

}