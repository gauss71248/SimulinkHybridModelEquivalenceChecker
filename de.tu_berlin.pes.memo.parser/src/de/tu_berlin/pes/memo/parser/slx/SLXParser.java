package de.tu_berlin.pes.memo.parser.slx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.conqat.lib.commons.collections.PairList;
import org.conqat.lib.simulink.builder.MDLSection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tu_berlin.pes.memo.MeMoPlugin;

/**
 * Parser for compressed or uncompressed simulink XML files
 *
 * @author Moritz, Marcus
 *
 */
public class SLXParser {
	private InputStream xmlfile;
	private InputStream configSetFile;
	private InputStream stateflowFile;
	private InputStream defaultsFile;

	private String modelName;

	// a map of <ID, BlockName>
	private Map<String, String> BlockToIDMap = new HashMap<String, String>();

	/**
	 * Creates new parser from compressed slx file
	 *
	 * @param zipFile
	 *           - the compressed file
	 */
	public SLXParser(ZipFile zipFile) {
		ZipEntry blockdiagram = zipFile.getEntry("simulink/blockdiagram.xml");
		if (blockdiagram == null) {
			throw new IllegalArgumentException("Malformed SLX-File...");
		}
		ZipEntry config = zipFile.getEntry("simulink/configSet0.xml");
		if(config != null) {
			try {
				configSetFile = zipFile.getInputStream(config);
			} catch (IOException e) {
				MeMoPlugin.logException(e.getMessage(), e);
			}
		}
		ZipEntry stateflow = zipFile.getEntry("simulink/stateflow.xml");
		if(stateflow != null) {
			try {
				stateflowFile = zipFile.getInputStream(stateflow);
			} catch(IOException e) {
				MeMoPlugin.logException(e.getMessage(), e);
			}
		}
		
		ZipEntry defaultt = zipFile.getEntry("simulink/bddefaults.xml");
		if(defaultt != null) {
			try {
				defaultsFile = zipFile.getInputStream(defaultt);
			} catch(IOException e) {
				MeMoPlugin.logException(e.getMessage(), e);
			}
		}
		
		try {
			xmlfile = zipFile.getInputStream(blockdiagram);
			this.modelName = zipFile.getName().substring(
					zipFile.getName().lastIndexOf(File.separatorChar) + 1,
					zipFile.getName().lastIndexOf("."));
		} catch (IOException e) {
			MeMoPlugin.logException(e.getMessage(), e);
		}
	}

	/**
	 * Creates new parser from uncompressed xml file
	 *
	 * @param xmlfile
	 */
	public SLXParser(File xmlfile) {
		try {
			this.xmlfile = new FileInputStream(xmlfile);
		} catch (FileNotFoundException e) {
			MeMoPlugin.logException(e.getMessage(), e);
		}
	}

	public MDLSection parse() throws Exception {
		
//		System.out.println("test");

		MDLSection section = parseXML();
		if (section == null) {
			throw new Exception("[ERROR] No valid model or library section found");
		}
		return section;
	}

	/**
	 * return the name of a block by its ID
	 *
	 * @param blockID
	 * @return the name of block as indicated by the ID, in case the ID does not
	 *         exist, an empty string
	 */
	private String getBlockNameByID(String blockID) {
		// iterate over all entries in the BlockID list
		for (Entry<String, String> e : this.BlockToIDMap.entrySet()) {
			// if the requested ID is found, return the corresponding block name
			if (e.getKey().equals(blockID)) {
				return e.getValue();
			}
		}
		// if the ID is not in the list, return an empty string
		return "";
	}

	/**
	 * For parsing Object section
	 *
	 * @param root
	 *           - the node where the section starts
	 * @return - the created section
	 */
	private MDLSection getObjectSection(Element root) {

		List<MDLSection> sections = new ArrayList<MDLSection>();
		PairList<String, String> parameter = new PairList<String, String>();

		// get all the attributes
		NamedNodeMap attr = root.getAttributes();

		String name = "";
		if (attr.getNamedItem("ClassName") == null) {
			name = "Object";
		} else {
			// use ClassName as name for the section
			name = attr.getNamedItem("ClassName").getNodeValue();
			// XXX: if uncommented, this causes errors
			// attr.removeNamedItem("ClassName");
		}

		for (int i = 0; i < attr.getLength(); i++) {
			// translate each attribute into a normal parameter
			parameter.add(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
		}

		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				// adds the P-Nodes as parameter
				if (e.getNodeName().equals("P")) {
					parseParameter(parameter, e);
				} else {
					sections.add(getSubsection(e)); // looking for subsections
				}
			}
		}

		return new MDLSection(name, sections, parameter, -1);
	}

	private void parseParameter(PairList<String, String> parameter, Element e) {
		String attr = e.getAttribute("Ref");
		if ((attr != null) && !attr.equals("")) {
			parameter.add(e.getAttribute("Name"), attr.split(":")[1]);
		} else {
			if(e.getAttribute("Name").equals("ModelNameDialog")) {
				parameter.add(e.getAttribute("Name"), e.getTextContent().replaceAll(".slx", ""));
			} else if (e.getAttribute("Name").equals("labelString")) {
				parameter.add(e.getAttribute("Name"), e.getTextContent().replaceAll("\n", "\\\\n"));
			} else {
				parameter.add(
						e.getAttribute("Name").replaceAll("SSID", "id")/*.replaceAll("subviewer", "chart")*/,
						e.getTextContent());
			}
		}

	}

	/**
	 * Creates a Array section
	 *
	 * @param root
	 * @return
	 */
	private MDLSection getArraySection(Element root) {
		List<MDLSection> sections = new ArrayList<MDLSection>();
		PairList<String, String> parameter = new PairList<String, String>();

		NamedNodeMap attr = root.getAttributes();

		// add each attribute as parameter
		for (int i = 0; i < attr.getLength(); i++) {
			parameter.add(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
		}

		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("P")) {
					parseParameter(parameter, e);
				} else if (e.getNodeName().equals("Cell")) {
					// add Cells as parameter
					parameter.add("Cell", e.getTextContent());
				} else {
					sections.add(getSubsection(e)); // looking for subsections
				}
			}
		}

		return new MDLSection("Array", sections, parameter, -1);
	}

	/**
	 * Creates a block section
	 *
	 * @param root
	 * @return
	 */
	private MDLSection getBlockSection(Element root) {
		List<MDLSection> sections = new ArrayList<MDLSection>();
		PairList<String, String> parameter = new PairList<String, String>();

		NamedNodeMap attr = root.getAttributes();

		String currentBlockID = "";
		String currentBlockName = "";

		// add each attribute as parameter
		for (int i = 0; i < attr.getLength(); i++) {
			// save the current block ID and name in local variables
			if (attr.item(i).getNodeName().equals("Name")) {
				// sanitize name parameter
				// replace line feed characters with spaces to avoid Matlab
				// confusion
				currentBlockName = attr.item(i).getNodeValue().replaceAll("\n", "\\\\n");

				parameter.add("Name", currentBlockName);
			} else if (attr.item(i).getNodeName().equals("SID")) {
				currentBlockID = attr.item(i).getNodeValue();
				// only if there is an SID attribute, save it to the HashMap
				// avoids creating empty entries for <Block> in the
				// <BlockParameterDefault> section
				this.BlockToIDMap.put(currentBlockID, currentBlockName);

				// add SID parameter
				parameter.add(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
			} else {
				// add all other parameters
				parameter.add(attr.item(i).getNodeName(), attr.item(i).getNodeValue());
			}
		}

		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("P")) {
					// add P-Nodes as parameter
					parseParameter(parameter, e);
				} else {
					sections.add(getSubsection(e)); // looking for subsections
				}
			}
		}

		return new MDLSection("Block", sections, parameter, -1);
	}

	/**
	 * Add a new signal line to parameters, based on sourceString
	 *
	 * @param sourceString
	 *           - string with the information about the signal line
	 * @param parameter
	 *           - parameter list where the signal line will be added
	 * @param src
	 *           - true if it is a srcblock, false otherwise
	 */
	private void addConvertedSignalLine(String sourceString, PairList<String, String> parameter,
			boolean src) {
		String Block = sourceString.split("#")[0]; // block of source block
		// String LineDirection = sourceString.split("#")[1].split(":")[0]; //
		// direction (in/out)
		String BlockPort = "";
		try {
			BlockPort = sourceString.split("#")[1].split(":")[1]; // port id
		} catch (Exception e) {
			BlockPort = sourceString.split("#")[1]; // check for other format
			/*
			 * //check for if-action if(sourceString.contains("ifaction")) {
			 * BlockPort = "ifaction"; } else if(sourceString.contains("state")) {
			 * BlockPort = "state"; }
			 */
		}

		if (src) {
			// as the source blocks cannot be saved by their ID but only by
			// their names, translate ID to name here
			parameter.add("SrcBlock", this.getBlockNameByID(Block));
			parameter.add("SrcPort", BlockPort);
		} else {
			// as the source blocks cannot be saved by their ID but only by
			// their names, translate ID to name here
			parameter.add("DstBlock", this.getBlockNameByID(Block));
			parameter.add("DstPort", BlockPort);
		}
	}

	private MDLSection getBranchSection(Element e) {
		List<MDLSection> branchsection = new ArrayList<MDLSection>();
		PairList<String, String> params = new PairList<String, String>();

		NodeList list = e.getChildNodes();
		for (int j = 0; j < list.getLength(); j++) {
			if (list.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element) list.item(j);
				if (el.getNodeName().equals("P")) {
					if (el.getAttribute("Name").equals("Dst")) {
						String DestinationStringContent = el.getTextContent();
						addConvertedSignalLine(DestinationStringContent, params, false);
					}
				} else if (el.getNodeName().equals("Branch")) {
					branchsection.add(getBranchSection(el));
				}
			}
		}
		return new MDLSection("Branch", branchsection, params, -1);
	}

	/**
	 * Creates new line section and converts the XML-Line definition into a
	 * MDL-Line definition
	 *
	 * @param root
	 * @return
	 */
	private MDLSection getLineSection(Element root) {
		List<MDLSection> sections = new ArrayList<MDLSection>();
		PairList<String, String> parameter = new PairList<String, String>();

		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("P")) {
					// add all P-Nodes as parameter

					// parameter.add(e.getAttribute("Name"),
					// e.getTextContent());
					// MeMoPlugin.out.println("\n" + e.getAttribute("Name") +
					// ", " + e.getTextContent());

					// check for Src and Dst tags and extract their content by
					// splitting the strings
					if (e.getAttribute("Name").equals("Src")) {
						// MeMoPlugin.out.println("Found source port for line...");
						String SourceStringContent = e.getTextContent();

						addConvertedSignalLine(SourceStringContent, parameter, true);
					} else if (e.getAttribute("Name").equals("Dst")) {
						// MeMoPlugin.out.println("Found destination port for line...");

						String DestinationStringContent = e.getTextContent();

						addConvertedSignalLine(DestinationStringContent, parameter, false);
					} else if (e.getAttribute("Name").equals("Name")) {
						parameter.add("Name", e.getTextContent());
					}
				} else if (e.getNodeName().equals("Branch")) { // got a branched
					// line, add new
					// branch
					// subsection
					sections.add(getBranchSection(e));
				} else {
					sections.add(getSubsection(e)); // looking for subsections
				}
			}
		}
		return new MDLSection("Line", sections, parameter, -1);
	}

	private MDLSection parseSingleNode(Element root) {
		if (root.getNodeName().equals("Object")) {
			return getObjectSection(root);
		} else if (root.getNodeName().equals("Array")) {
			return getArraySection(root);
		} else if (root.getNodeName().equals("Block")) {
			return getBlockSection(root);
		} else if (root.getNodeName().equals("Line")) {
			return getLineSection(root);
		}

		return null;
	}

	/**
	 * Creates a subsection from the given XML-Node
	 *
	 * @param root
	 * @return
	 */
	private MDLSection getSubsection(Element root) {
		List<MDLSection> sections = new ArrayList<MDLSection>();
		PairList<String, String> parameters = new PairList<String, String>();

		if (root.getNodeName().equals("ConfigurationSet")) {
			MDLSection cur = parseSingleNode((Element) root.getElementsByTagName("Array").item(0));
			if (cur != null) {
				return cur;
			}
		}

		MDLSection cur = parseSingleNode(root);
		if (cur != null) {
			return cur;
		}

		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("P")) {
					parseParameter(parameters, e);
				} else if (e.getNodeName().equals("Object")) {
					sections.add(getObjectSection(e));
				} else if (e.getNodeName().equals("Array")) {
					sections.add(getArraySection(e));
				} else if (e.getNodeName().equals("Block")) {
					sections.add(getBlockSection(e));
				} else if (e.getNodeName().equals("Line")) {
					sections.add(getLineSection(e));
				} else {
					sections.add(getSubsection(e)); // looking for subsection
				}
			}
		}
		
		return new MDLSection(root.getNodeName(), sections, parameters, -1);
	}

	/**
	 * Find all blocks with it's ids in the document and fills BlockToIDMap
	 *
	 * @param root
	 */
	private void fillBlockIdMap(Element root) {
		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("Block")) {
					getBlockSection(e);
				} else {
					fillBlockIdMap(e);
				}
			}
		}
	}

	/**
	 * Creates a MDL-Section from the root-node of the XML doc
	 *
	 * @param name
	 *           - name of the section
	 * @param root
	 *           - root node
	 * @param isRoot
	 *           - add name parameter, if is root node
	 * @return
	 */
	private MDLSection getMDLSection(String name, Element root, boolean isRoot) {
		fillBlockIdMap(root);

		MDLSection section = getSubsection(root);

		List<MDLSection> sections = new ArrayList<MDLSection>(section.getSubSections());
		PairList<String, String> parameters = new PairList<String, String>();

		for (String s : section.getParameterNames()) {
			parameters.add(s, section.getParameter(s));
		}

		if (isRoot) {
			parameters.add("Name", modelName); // add modelName
		}

		section = new MDLSection(name, sections, parameters, -1);

		return section;
	}

	private MDLSection parseXML() {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlfile);

			// see
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();

			List<MDLSection> sections = new ArrayList<MDLSection>();

			// MDLSection tmp = getMDLSection("", root, false);

			// get name of the model
			/*
			 * this.modelName = tmp.getSubSections("Model").get(0)
			 * .getSubSections("SimulationSettings").get(0)
			 * .getParameterMapRecursively()
			 * .get("Simulink.SimulationData.ModelLoggingInfo.model_");
			 */

			MDLSection model = null;
			
			if (root.getElementsByTagName("Model").getLength() > 0) {
				model = getMDLSection("Model", (Element) root.getElementsByTagName("Model").item(0),
						true);
			} else if (root.getElementsByTagName("Library").getLength() > 0) {
				model = getMDLSection("Library",
						(Element) root.getElementsByTagName("Library").item(0), true);
			}

			if (model != null) {
				if(configSetFile != null) {
					List<MDLSection> sec = new ArrayList<MDLSection>(model.getSubSections());
					DocumentBuilderFactory dbFactory2 = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder2 = dbFactory2.newDocumentBuilder();
					Document doc2 = dBuilder2.parse(configSetFile);
					doc2.getDocumentElement().normalize();

					Element root2 = doc2.getDocumentElement();
					MDLSection ConfigObject = getSubsection((Element)root2.getElementsByTagName("Object").item(0));
					
					// create dummy array node
					// like <Array PropName="ConfigurationSets" Type="Handle" Dimension="1*1">
					PairList<String,String> arrParams = new PairList<String, String>();
					arrParams.add("PropName", "ConfigurationSets");
					arrParams.add("Type","Handle");
					arrParams.add("Dimension","1*1");
					
					List<MDLSection> l = new ArrayList<MDLSection>(1);
					l.add(ConfigObject);
					MDLSection array = new MDLSection("Array", l, arrParams, -1);
					
					sec.add(array);
					PairList<String, String> par = new PairList<String, String>();
					for(String p : model.getParameterNames()) {
						par.add(p, model.getParameter(p));
					}

					model = new MDLSection(model.getName(), sec, par, -1);
				}
				
				if(defaultsFile != null) {
					List<MDLSection> sec = new ArrayList<MDLSection>(model.getSubSections());
					
					DocumentBuilderFactory dbFactory2 = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder2 = dbFactory2.newDocumentBuilder();
					Document doc2 = dBuilder2.parse(defaultsFile);
					doc2.getDocumentElement().normalize();

					Element root2 = doc2.getDocumentElement();
					
					NodeList l = root2.getChildNodes();
					for(int i=0;i<l.getLength();i++) {
						Node n = l.item(i);
						if(n instanceof Element) {
							Element e = (Element)n;
							MDLSection d = getSubsection(e);
							sec.add(d);
						}
						
					}
					
					PairList<String, String> par = new PairList<String, String>();
					for(String p : model.getParameterNames()) {
						par.add(p, model.getParameter(p));
					}

					model = new MDLSection(model.getName(), sec, par, -1);
				}
				
				sections.add(model);
				
				
			} else {
				return null;
			}
			
			if(stateflowFile != null) {
				DocumentBuilderFactory dbFactory2 = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder2 = dbFactory2.newDocumentBuilder();
				Document doc2 = dBuilder2.parse(stateflowFile);
				doc2.getDocumentElement().normalize();

				Element root2 = doc2.getDocumentElement();
				Element sf = (Element) root2;
				if (sf != null) {
					sections.add(getStateFlowSection("0", "0", null, sf));
				}
			} else {
				Element sf = (Element) root.getElementsByTagName("Stateflow").item(0);
				if (sf != null) {
					sections.add(getStateFlowSection("0", "0", null, sf));
				}
			}
			// System.out.println(this.BlockToIDMap);

			return new MDLSection("", sections, new PairList<String, String>(), -1);
		} catch (ParserConfigurationException e) {
			MeMoPlugin.logException(e.getMessage(), e);
		} catch (SAXException e) {
			MeMoPlugin.logException(e.getMessage(), e);
		} catch (IOException e) {
			MeMoPlugin.logException(e.getMessage(), e);
		}

		return null;
	}

	private MDLSection getStateFlowSection(String parent_tree_ID, String parent_linkNode_ID, List<MDLSection> parent, Element root) {
		List<MDLSection> sections = new ArrayList<MDLSection>();
		PairList<String, String> parameters = new PairList<String, String>();

		if (parent == null) {
			parent = sections;
		}

		String name = root.getNodeName();

		if (name.equals("machine")) {
			parameters.add("name", this.modelName);
		}

		String this_tree_parentID = parent_tree_ID;
		String this_linkNode_parentID = parent_linkNode_ID;

		for (int i = 0; i < root.getAttributes().getLength(); i++) {
			Node n = root.getAttributes().item(i);
			if (n.getNodeName().equals("SSID") || n.getNodeName().equals("id")) {
				this_tree_parentID = n.getNodeValue();
				parameters.add("id", n.getNodeValue());
				if(name.equals("chart")) {
					this_linkNode_parentID = this_tree_parentID;
				}
			} else {
				parameters.add(n.getNodeName(), n.getNodeValue());
			}
		}
		parameters.add("treeNode", "[" + parent_tree_ID + " 0 0 0]");
		parameters.add("linkNode", "[" + parent_linkNode_ID + " 0 0]");
		parameters.add("chart", parent_linkNode_ID);

		Stack<Node> nodes = new Stack<Node>();
		NodeList l = root.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			nodes.push(l.item(i));
		}

		while (nodes.size() > 0) {
			Node n = nodes.pop();
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("Children")) {
					NodeList list = e.getChildNodes();
					for (int j = 0; j < list.getLength(); j++) {
						Node node = list.item(j);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							parent.add(getStateFlowSection(this_tree_parentID, this_linkNode_parentID, parent, (Element) node));
						}
					}
				} else if (e.getNodeName().equals("P")) {
					parseParameter(parameters, e);
				} else {
					sections.add(getStateFlowSection(this_tree_parentID, this_linkNode_parentID, parent, e));
				}
			}
		}

		return new MDLSection(name, sections, parameters, -1);
	}
}
