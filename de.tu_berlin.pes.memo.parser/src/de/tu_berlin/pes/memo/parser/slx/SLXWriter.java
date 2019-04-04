package de.tu_berlin.pes.memo.parser.slx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.Branch;
import org.hibernate.mapping.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.parser.slx.opc.OPCWriter;

/*
 * SLX files are simple zip files with some xml in it (see Open Packaging Conventions for more info)
 * 
 * basic structure:
 * * model.slx/
 *   * metadata/
 *     * coreProperties.xml
 *     * mwcoreProperties.xml
 *     * thumbnail.png
 *   * _rels/
 *     * .rels
 *   * simulink/
 *     * _rels/
 *       * blockdiagram.xml.rels
 *     * blockdiagram.xml
 *     * graphicalInterface.xml
 *   * [Content_Types].xml
 */

public class SLXWriter {
	private Model model;
	private File file;
	
	private Map<Block,Long> blockMap = new HashMap<Block,Long>();
	private Map<Block,List<Block>> subsystemMap = new HashMap<Block,List<Block>>();
	
	public SLXWriter(Model model, File file) {
		this.model = model;
		this.file = file;
	}
	
	public void writeModel() throws IOException, ParserConfigurationException, TransformerException {		
		OPCWriter w = new OPCWriter(new FileOutputStream(file));
		w.addDefaultType("image/png", "png");
		w.addDefaultType("application/vnd.openxmlformats-package.relationships+xml", "rels");
		w.addDefaultType("application/vnd.mathworks.simulink.mdl+xml", "xml");
		w.addOverrideType("application/vnd.openxmlformats-package.core-properties+xml", "/metadata/coreProperties.xml");
		w.addOverrideType("application/vnd.mathworks.package.coreProperties+xml", "/metadata/mwcoreProperties.xml");
		w.addOverrideType("application/vnd.mathworks.simulink.graphicalInterface+xml", "/simulink/graphicalInterface.xml");
		
		/*w.addRelationship("Thumbnail", "metadata/thumbnail.png", "http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail");
		w.addRelationship("blockDiagram","simulink/blockdiagram.xml","http://schemas.mathworks.com/simulink/2010/relationships/blockDiagram");
		w.addRelationship("coreprops","metadata/coreProperties.xml","http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties");
		w.addRelationship("rId1","metadata/mwcoreProperties.xml","http://schemas.mathworks.com/package/2012/relationships/coreProperties");*/
		final String blocks = createBlockdiagramXML();
		w.addContent("simulink/blockdiagram.xml", new InputStream() {
			int i=0;
			
			@Override
			public int read() throws IOException {
				if(i>=blocks.length()) {
					return -1;
				}
				return blocks.charAt(i++);
			}
		});
		w.flush();
		w.close();
	}
	
	public Element createParameterElement(Document doc, String name, String value) {
		Element P = doc.createElement("P");
		P.setAttribute("Name", name);
		P.setTextContent(value);
		return P;
	}
	
	private static Map<String,Set<String>> validParam;
	
	private static HashSet<String> getHashSet(String[] strings) {
		return new HashSet<String>(Arrays.asList(strings));
	}
	
	private static void addValidParam(String blockname, String... strings) {
		if(validParam.containsKey(blockname)) {
			validParam.get(blockname).addAll(getHashSet(strings));
		} else {
			validParam.put(blockname, getHashSet(strings));
		}
	}
	
	static {
		//TODO add more params
		validParam = new HashMap<String, Set<String>>();
		//[ALL_BLOCKS] is a placeholder for all blocks
		addValidParam("[ALL_BLOCKS]",
				"Value",
				"Position",
				"ZOrder",
				"Floating",
				"Location",
				"Open",
				"NumInputPorts",
				"IconDisplay",
				"Port",
				"ShowName",
				"Ports");
		// Gain block
		addValidParam("Gain", "Gain");
		// sum block
		addValidParam("Sum", "Inputs", "IconShape");
		// Reference
		addValidParam("Reference", "SourceBlock", "SourceType");
		// SignalGenerator
		addValidParam("SignalGenerator", "WaveForm", "Frequency", "Units");
		// TransferFcn
		addValidParam("TransferFcn", "Denominator", "Numerator");
		// Bandnoise
		addValidParam("Reference", "Cov", "Ts", "seed", "VectorParams1D");
		// Outport
		addValidParam("Outport", "InitialOutput");
	}
	
	private Set<String> floatParams = new HashSet<String>(Arrays.asList((new String[] {
		"Ts"
	})));
	Pattern arrayRegex = Pattern.compile("^\\[([0-9. a-zA-Z\\-]+)\\]$");
	private boolean isArrayParam(String p) {
		return arrayRegex.matcher(p).matches();
	}
	
	private String fixArray(String paramvalue, String param) {
		if(isArrayParam(paramvalue)) {
			Matcher m = arrayRegex.matcher(paramvalue);
		   m.matches();
		   String gr = m.group(1);
			String[] split = m.group(1).split(" ");

			String ret = "[";
			if(split.length==2) {
				switch(param) {
					default: break;
					case "ModelBrowserWidth":
					case "TiledPageScale":
					return split[1];
					
				}
			}
			if(split.length == 1 && split[0] != null && !split[0].isEmpty()) {
				return "["+split[0]+"]";
			}
			
			for(int i=1;i<split.length;i++) {
				if(i <split.length-1) {
					try {
						if(floatParams.contains(param)) {
							ret += (Float.parseFloat(split[i])) + ", ";
						} else {
							ret += ((int)Float.parseFloat(split[i])) + ", ";
						}
					} catch (NumberFormatException ex) {
						ret += split[i] + ", ";
					}
				} else {
					try {
						if(floatParams.contains(param)) {
							ret += (Float.parseFloat(split[i]));
						} else {
							ret += ((int)Float.parseFloat(split[i]));
						}
					} catch (NumberFormatException ex) {
						ret += split[i];
					}
				}
			}
			ret += "]";
			return ret;
		}
		return paramvalue;
	}
	
	private boolean checkParam(Block b, String param) {
		return ((validParam.get(b.getType()) != null &&
				validParam.get(b.getType()).contains(param)) || 
				(validParam.get("[ALL_BLOCKS]") != null && validParam.get("[ALL_BLOCKS]").contains(param))) &&
				b.getParameter(param) != null && 
				!b.getParameter(param).equals("") &&
				!b.getParameter(param).equals("[]");
	}
	
	public void addBlock(Document doc, Element parent, Block b) {
		if(b.getParent() != null && b.getParent() instanceof Block) {
			//handle subsystem
			List<Block> subsystemBlocks = subsystemMap.get(b.getParent());
			if(subsystemBlocks == null) {
				subsystemBlocks = new ArrayList<Block>();
				subsystemMap.put((Block) b.getParent(), subsystemBlocks);
				subsystemBlocks.add(b);
			} else {
				subsystemBlocks.add(b);
			}
			return;
		}
		if(b.getType().equals("SubSystem")) {
			return;
		}
		Element block = doc.createElement("Block");
		block.setAttribute("BlockType", b.getType());
		block.setAttribute("Name", b.getName().replaceAll("\\\\n", " "));
		block.setAttribute("SID", Long.toString(blockMap.get(b)));
		
		for(String p : b.getParameter().keySet()) {
			if(checkParam(b, p)) {
				block.appendChild(createParameterElement(doc, p, fixArray(b.getParameter(p),p)));
			}
		}
		
		parent.appendChild(block);
		
		for(Block subsystem : subsystemMap.keySet()) {
			addBlock(doc, parent, subsystem);
		}
	}
	
	private void addSubSystem(Document doc, Element parent, Block b, List<Block> subblocks) {
		Element block = doc.createElement("Block");
		block.setAttribute("BlockType", b.getType());
		block.setAttribute("Name", b.getName().replaceAll("\\\\n", " "));
		block.setAttribute("SID", Long.toString(blockMap.get(b)));
		
		//map blocks to generated ids
		//Map<Block, Long> subblockMap = new HashMap<Block, Long>();
		
		for(String p : b.getParameter().keySet()) {
			if(checkParam(b, p)) {
				block.appendChild(createParameterElement(doc, p, fixArray(b.getParameter(p),p)));
			}
		}
		block.appendChild(createParameterElement(doc, "Ports", "["+b.getInPorts().size()+", "+b.getOutPorts().size()+"]"));
		
		Element SubSystem = doc.createElement("System");
		for(String p : b.getParameter().keySet()) {
			if(checkParam(b, p)) {
				SubSystem.appendChild(createParameterElement(doc, p, fixArray(b.getParameter(p),p)));
			}
		}
		block.appendChild(SubSystem);
		
		parent.appendChild(block);
		for(Block subblock : subblocks) {
			// we are in a subsystem and found another subsystem, add this
			if(subsystemMap.get(subblock) != null) {
				addSubSystem(doc, SubSystem, subblock, subsystemMap.get(subblock));
				continue;
			}
			
			Element SubBlock = doc.createElement("Block");
			SubBlock.setAttribute("BlockType", subblock.getType());
			SubBlock.setAttribute("Name", subblock.getName().replaceAll("\\\\n", " "));
			SubBlock.setAttribute("SID", Long.toString(blockMap.get(subblock)));
			
			for(String p : subblock.getParameter().keySet()) {
				if(checkParam(subblock, p)) {
					SubBlock.appendChild(createParameterElement(doc, p, fixArray(subblock.getParameter(p),p)));
				}
			}
			SubSystem.appendChild(SubBlock);
		}
		//maps lines with the same source block to a list of signal lines
		Map<Block,List<SignalLine>> branchedSignalLines = new HashMap<Block, List<SignalLine>>();
		
		for(SignalLine l : model.getSignalLines()) {
			if(isSubBlock(l, b, subblocks)) {
				if(blockMap.get(l.getSrcBlock()) != null && blockMap.get(l.getDstBlock()) != null) {
					//we've got a previous source block again, add the extra signalline
					if(branchedSignalLines.get(l.getSrcBlock()) != null) {
						branchedSignalLines.get(l.getSrcBlock()).add(l);
					} else {
						branchedSignalLines.put(l.getSrcBlock(), new ArrayList<SignalLine>());
						branchedSignalLines.get(l.getSrcBlock()).add(l);
					}
				}
			}
		}
		for(Block src : branchedSignalLines.keySet()) {
			SubSystem.appendChild(createLine(doc, branchedSignalLines.get(src),blockMap));
		}
	}
	
	/**
	 * checks if a given signal line's src and dst block are in scope of the given parent block
	 * @param l
	 * @param parent
	 * @param subblocks
	 * @return
	 */
	private boolean isSubBlock(SignalLine l, Block parent, List<Block> subblocks) {
//		return (l.getSrcBlock().getParent() == parent ||
//						(subblocks.contains(l.getSrcBlock().getParent()) &&
//								l.getSrcBlock().getParent() != null && 
//								l.getSrcBlock().getParent() instanceof Block &&
//								!((Block)l.getSrcBlock().getParent()).getType().equals("SubSystem")) ||
//								l.getSrcBlock().getType().equals("Outport")) &&
//				 (l.getDstBlock().getParent() == parent ||
//				 		subblocks.contains(l.getDstBlock().getParent()) &&
//				 				l.getDstBlock().getParent() != null &&
//				 				l.getDstBlock().getParent() instanceof Block &&
//				 				!((Block)l.getDstBlock().getParent()).getType().equals("SubSystem") ||
//				 				l.getDstBlock().getType().equals("Inport"));
		//src is in current context
		boolean srcInContext = l.getSrcBlock().getParent() == parent;
		//dst is in current context
		boolean dstInContext = l.getDstBlock().getParent() == parent;
		//src is in submodel
		boolean srcSubBlock = subblocks.contains(l.getSrcBlock().getParent());
		//dst is in submodel
		boolean dstSubBlock = subblocks.contains(l.getDstBlock().getParent());
		//src is outport
		boolean srcIsOutport = l.getSrcBlock() instanceof Block && ((Block)l.getSrcBlock()).getType().equals("Outport");
		//dst is inport
		boolean dstIsInport = l.getDstBlock() instanceof Block && ((Block)l.getDstBlock()).getType().equals("Inport");
		
		//check if src and dst are in the same context
		if(srcInContext && dstInContext) return true;
		//check if src is in current context, but dst is in submodel and dst is inport
		if(srcInContext && dstSubBlock && dstIsInport) return true;
		//check if dst is in current context, but src is in submodel and src is outport
		if(dstInContext && srcSubBlock && srcIsOutport) return true;
		
		return false;
	}
	
	private Integer[] parseSimulinkArray(String arr) {
		arr = arr.replaceAll("\\[|\\]", "");
		List<Integer> list = Arrays.asList(arr.split(",|, | ")).stream()
			.filter(s -> !s.isEmpty())
			.map(Float::parseFloat)
			.map(f -> (int)(float)f).collect(Collectors.toList());											
		
		return list.toArray(new Integer[list.size()]);
	}
	
	private Set<String> validLineParam = new HashSet<String>(Arrays.asList((new String[] {
			"Name",
			"ZOrder",
			"Labels"
	})));
	
	public Element createLine(Document doc, List<SignalLine> lines, Map<Block,Long> blockMap) {
		if(lines.size() == 0) return null;
		
		Element Line = doc.createElement("Line");
		//Line.appendChild(createParameterElement(doc, "ZOrder", "1"));
		String srcStr = "";
		SignalLine l = lines.get(0);
		Block src = l.getSrcBlock();
		
		if(src.getType().equals("Outport")) {
			srcStr = blockMap.get(src.getParent())+"#out:"+src.getParameter("Port");
		} else {
			srcStr = blockMap.get(l.getSrcBlock())+"#out:"+l.getSrcPort().getNumber();
		}
		Line.appendChild(createParameterElement(doc, "Src", srcStr));
		if(lines.size() == 1) {
			if(l.getName() != null) {
				Line.appendChild(createParameterElement(doc, "Name", l.getName()));
			}
			String dstStr = "";
			Block dst = l.getDstBlock();
			if(dst.getType().equals("Inport")) {
				dstStr = blockMap.get(dst.getParent())+"#in:"+dst.getParameter("Port");
			} else {
				dstStr = blockMap.get(l.getDstBlock())+"#in:"+l.getDstPort().getNumber();
			}
			Line.appendChild(createParameterElement(doc, "Dst", dstStr));
		} else {
			int x,y;
			int left, top, right, bottom;
			int left1, top1, right1, bottom1;
			Integer[] coords = parseSimulinkArray(src.getParameter("Position"));
			left = coords[0];
			top = coords[1];
			right = coords[2];
			bottom = coords[3];
			
			Block dst1 = l.getDstBlock();
			Integer[] coords1 = parseSimulinkArray(dst1.getParameter("Position"));
			left1 = coords1[0];
			top1 = coords1[1];
			right1 = coords1[2];
			bottom1 = coords1[3];
			//TODO: change this hack & use the values from above
			x = 10;//Math.abs(left-left1);
			y = 0;
			Line.appendChild(createParameterElement(doc, "Points", "["+x+" "+y+"]"));
			for(SignalLine line : lines) {
				Element Branch = doc.createElement("Branch");
				if(line.getName() != null) {
					Branch.appendChild(createParameterElement(doc, "Name", line.getName()));
				}
				//Branch.appendChild(createParameterElement(doc, "ZOrder", "1"));
				String dstStr = "";
				Block dst = line.getDstBlock();
				if(dst.getType().equals("Inport")) {
					dstStr = blockMap.get(dst.getParent())+"#in:"+dst.getParameter("Port");
				} else {
					dstStr = blockMap.get(line.getDstBlock())+"#in:"+line.getDstPort().getNumber();
				}
				Branch.appendChild(createParameterElement(doc, "Dst", dstStr));
				Line.appendChild(Branch);
			}
		}
		return Line;
	}
	
	public String createBlockdiagramXML() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		// root elements
		Document doc = docBuilder.newDocument();
		doc.setXmlStandalone(true);
		doc.setXmlVersion("1.0");
		
		Element ModelInformation = doc.createElement("ModelInformation");
		ModelInformation.setAttribute("Version", "1.0");
		
		doc.appendChild(ModelInformation);
		
		Element Model = doc.createElement("Model");
		ModelInformation.appendChild(Model);
		
		//Encoding the xml without this doesn't work
		Model.appendChild(createParameterElement(doc, "SavedCharacterEncoding", "UTF-8"));
		
		Element System = doc.createElement("System");
		Model.appendChild(System);
		
		System.appendChild(createParameterElement(doc, "Location", "[1,1,1,1]"));
		System.appendChild(createParameterElement(doc, "Open", "on"));
		System.appendChild(createParameterElement(doc, "ModelBrowserWidth", "200"));
		System.appendChild(createParameterElement(doc, "TiledPaperMargins", "[1.270000, 1.270000, 1.270000, 1.270000]"));
		System.appendChild(createParameterElement(doc, "TiledPageScale", "1"));
		System.appendChild(createParameterElement(doc, "ZoomFactor", "100"));
		System.appendChild(createParameterElement(doc, "ReportName", "simulink-defaul.rpt"));
		
		long bid = 1;
		for(Block b : model.getBlocks()) {
			blockMap.put(b, bid);
			bid++;
		}
		Element SIDHW = createParameterElement(doc, "SIDHighWatermark", Long.toString(bid));
		System.appendChild(SIDHW);
		
		for(Block b : model.getBlocks()) {
			addBlock(doc, System, b);
		}
		SIDHW.setTextContent(Long.toString(bid));
		
		for(Block subsystem : subsystemMap.keySet()) {
			if(subsystem.getParent() instanceof Model) {
				addSubSystem(doc,System,subsystem,subsystemMap.get(subsystem));
			}
		}
		
		//maps lines with the same source block to a list of signal lines
		Map<Block,List<SignalLine>> branchedSignalLines = new HashMap<Block, List<SignalLine>>();
		
		for(SignalLine l : model.getSignalLines()) {
			Block src = l.getSrcBlock();
			Block dst = l.getDstBlock();
//			if(src.getType().equals("Outport")) {
// 				src = (Block) src.getParent();
//			}
//			if(dst.getType().equals("Inport")) {
//				dst = (Block) dst.getParent();
//			}
			if(blockMap.get(src) != null && blockMap.get(dst) != null) {
				//we've got a previous source block again, add the extra signalline
				if(branchedSignalLines.get(l.getSrcBlock()) != null) {
					branchedSignalLines.get(l.getSrcBlock()).add(l);
				} else {
					branchedSignalLines.put(l.getSrcBlock(), new ArrayList<SignalLine>());
					branchedSignalLines.get(l.getSrcBlock()).add(l);
				}
			}
		}
		for(Block src : branchedSignalLines.keySet()) {
			System.appendChild(createLine(doc, branchedSignalLines.get(src),blockMap));
		}
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StringWriter w = new StringWriter();
		StreamResult result = new StreamResult(w);
 
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);
 
		transformer.transform(source, result);
		
		return w.toString();
	}
}
