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

package de.tu_berlin.pes.memo.parser.stateflow.testenvironment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * CREATES an Graphical-User-Interface for the Parser
 */
public class WrapperGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JButton mdlFileBtn;
	private JTextField mdlFilePath;
	private JButton mdlFolderBtn;
	private JTextField mdlFolderPath;
	private JButton outputSaveBtn;
	private JTextField outputSavePath;
	private JButton runBtn;
	private final Point WINDOW_POSITION = new Point(300, 300);
	private String modelPath;
	private Boolean startParse = false;

	/**
	 * cwd is the File that Contains the path information to the models
	 * IMPORTANT: whithout setting any path it points to the major project path
	 * (what means-all models in the project-folder or in any subfolder are
	 * parsed by pressing the parse button)
	 */
	private File cwd = new File(".");

	/**
	 * GUI Function that Creates the Window
	 */
	public WrapperGUI() {
		super("ParserGUI");
		createWindowElements();
		this.setLocation(WINDOW_POSITION);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
		this.pack();
		this.setResizable(false);
	}

	/**
	 * GUI Function that Creates the Content of the Window
	 */
	private void createWindowElements() {
		GridBagLayout gbl = new GridBagLayout();
		this.setLayout(gbl);

		GridBagConstraints constr;

		mdlFileBtn = new JButton("Parse mdl File");
		mdlFileBtn.addActionListener(this);
		this.add(mdlFileBtn);
		constr = new GridBagConstraints();
		constr.insets = new Insets(20, 10, 0, 0);
		constr.gridx = 2;
		constr.gridy = 0;
		constr.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(mdlFileBtn, constr);

		mdlFilePath = new JTextField(50);
		mdlFilePath.setAutoscrolls(true);
		this.add(mdlFilePath);
		constr.insets = new Insets(20, 0, 0, 0);
		constr.gridwidth = 2;
		constr.gridx = 0;
		constr.gridy = 0;
		gbl.setConstraints(mdlFilePath, constr);

		mdlFolderBtn = new JButton("Parse mdl folder");
		mdlFolderBtn.addActionListener(this);
		this.add(mdlFolderBtn);
		constr = new GridBagConstraints();
		constr.insets = new Insets(20, 10, 0, 0);
		constr.gridx = 2;
		constr.gridy = 2;
		constr.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(mdlFolderBtn, constr);

		mdlFolderPath = new JTextField(50);
		mdlFolderPath.setAutoscrolls(true);
		this.add(mdlFolderPath);
		constr.insets = new Insets(20, 0, 0, 0);
		constr.gridwidth = 2;
		constr.gridx = 0;
		constr.gridy = 2;
		gbl.setConstraints(mdlFolderPath, constr);

		outputSaveBtn = new JButton("Select Output File");
		outputSaveBtn.addActionListener(this);
		this.add(outputSaveBtn);
		constr = new GridBagConstraints();
		constr.insets = new Insets(20, 10, 0, 0);
		constr.gridx = 2;
		constr.gridy = 4;
		constr.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(outputSaveBtn, constr);

		outputSavePath = new JTextField(50);
		outputSavePath.setAutoscrolls(true);
		this.add(outputSavePath);
		constr.insets = new Insets(20, 0, 0, 0);
		constr.gridwidth = 2;
		constr.gridx = 0;
		constr.gridy = 4;
		gbl.setConstraints(outputSavePath, constr);

		runBtn = new JButton("Parse");
		runBtn.addActionListener(this);
		this.add(runBtn);
		constr = new GridBagConstraints();
		constr.insets = new Insets(20, 30, 0, 0);
		constr.gridx = 2;
		constr.gridy = 6;
		constr.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(runBtn, constr);
	}

	// @Override
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mdlFileBtn) {
			executeMdlFileBrowse();
		} else if (e.getSource() == mdlFolderBtn) {
			executeMdlFolderBrowse();
		} else if (e.getSource() == outputSaveBtn) {
			executeOutputFileBrowse();
		} else if (e.getSource() == runBtn) {
			executeParse();
		}

	}

	/**
	 * Simple GUI Function that Selects a *.mdl file Called by the GUI
	 * MDL-BROWSE-BUTTON
	 */
	public void executeMdlFileBrowse() {
		JFileChooser fc = new JFileChooser(cwd);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "Simulink Model File";
			}

			@Override
			public boolean accept(File f) {
				return f.getAbsolutePath().endsWith(".mdl") || f.isDirectory();
			}
		});
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				modelPath = fc.getSelectedFile().getCanonicalPath();
				mdlFilePath.setText(modelPath);
			} catch (IOException e1) {
				e1.printStackTrace();
				logException(e1);
			}
		}
	}

	/**
	 * Simple GUI Function that Selects a folder Called by the GUI
	 * MDL-FOLDER-BROWSE-BUTTON
	 */
	public void executeMdlFolderBrowse() {
		JFileChooser fc = new JFileChooser(cwd);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "Simulink Model Folder";
			}

			@Override
			public boolean accept(File f) {
				return true;
			}
		});
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				modelPath = new String(fc.getSelectedFile().getCanonicalPath());
				mdlFolderPath.setText(modelPath);
			} catch (IOException e1) {
				e1.printStackTrace();
				logException(e1);
			}
		}
	}

	public void executeOutputFileBrowse() {
		JFileChooser fc = new JFileChooser(cwd);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		FileFilter filter = new FileFilter() {

			@Override
			public String getDescription() {
				return ".txt File";
			}

			@Override
			public boolean accept(File f) {
				return true;
			}
		};

		fc.setFileFilter(filter);
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				String outputPath = new String(fc.getSelectedFile().getCanonicalPath());
				outputSavePath.setText(outputPath);
			} catch (IOException e1) {
				e1.printStackTrace();
				logException(e1);
			}
		}
	}

	public void getTextfieldInput() {

		/* For Standart Textfield Input, MDLFILE Textfield is taken */
		if (!mdlFilePath.getText().isEmpty()) {
			cwd = new File(mdlFilePath.getText());
			ParserWrapper.log("INPUT SPECIFIED.", ParserWrapper.LogType.FINE);
		} else if (!mdlFolderPath.getText().isEmpty()) {
			cwd = new File(mdlFolderPath.getText());
			ParserWrapper.log("INPUT SPECIFIED.", ParserWrapper.LogType.FINE);
		} else {
			ParserWrapper.log("ERROR: NO INPUT SPECIFIED.TAKING ROOT FOLDER FILES AS DEFAULTINPUT.",
					ParserWrapper.LogType.SEVERE);
		}

		if (!outputSavePath.getText().isEmpty()) {
			ParserWrapper.log("OUTPUT SPECIFIED.", ParserWrapper.LogType.FINE);
			startParse = true;
		} else {
			startParse = false;
			ParserWrapper.log("ERROR: NO OUTPUT SPECIFIED.PARSING NOT STARTED.",
					ParserWrapper.LogType.SEVERE);
		}
	}

	public void logException(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		ParserWrapper.log(sw.toString(), ParserWrapper.LogType.SEVERE);
	}

	/**
	 * Executes extracting and parsing of the all models of the path variable
	 * cwd. Called by the GUI PARSE-BUTTON
	 */
	public void executeParse() {

		ArrayList<String> state_errorlist = new ArrayList<String>();
		ArrayList<String> transition_errorlist = new ArrayList<String>();
		ArrayList<String> model_list;

		getTextfieldInput();
		if (startParse == true) {
			ParserWrapper.log("START TO PARSE VIA GUI", ParserWrapper.LogType.FINEST);
			ParserWrapper.log("FILE/FOLDER to PARSE:" + cwd.toString(), ParserWrapper.LogType.FINEST);
			model_list = ParserWrapper.findModels(cwd);

			ParserWrapper.printStringList(model_list);

			if (model_list.isEmpty()) {
				ParserWrapper.log("NO MODELS TO PARSE.CHECK CORRECT MODEL ARGUMENT.",
						ParserWrapper.LogType.SEVERE);
			} else {
				for (String model : model_list) {
					state_errorlist
							.addAll(ParserWrapper.parse(ParserWrapper.readStates(model), "state"));
					transition_errorlist.addAll(ParserWrapper.parse(
							ParserWrapper.readTransitions(model), "transition"));
				}

				ParserWrapper.setDebug(true);

				ParserWrapper.writeToFile(state_errorlist, transition_errorlist,
						outputSavePath.getText());
				ParserWrapper.log("PARSE FINISHED.", ParserWrapper.LogType.FINEST);

				ParserWrapper.setDebug(false);
				JOptionPane.showMessageDialog(null, "PARSERUN FINISHED.", "RESULT",
						JOptionPane.OK_CANCEL_OPTION);
			}
		} else {
			ParserWrapper
					.log("ERROR: START TO PARSE VIA GUI NOT POSSIBLE DUE TO INSUFFICIENT INPUT OR OUTPUT ARGUMENT",
							ParserWrapper.LogType.SEVERE);
		}
	}
}
