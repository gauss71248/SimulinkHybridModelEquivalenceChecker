package de.tu_berlin.pes.memo.parser.slx.opc;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class implements a generic ISO/IEC 29500:2008 and ECMA-376
 * Open Packaging Conventions package writer
 * @author moritz
 *
 */
public class OPCWriter implements Closeable, Flushable, AutoCloseable{
	public enum Type {
		DEFAULT, OVERRIDE
	};
	
	private List<ContentType> contentTypes;
	private List<Relationship> relationships;
	private Map<String, List<Relationship>> partRelationships;
	
	private Map<String,InputStream> content;
	private Map<String, String> coreProperties;
	
	private ZipOutputStream zip;

	
	public OPCWriter(OutputStream out) {
		contentTypes = new ArrayList<ContentType>();
		relationships = new ArrayList<Relationship>();
		partRelationships = new HashMap<String, List<Relationship>>();
		coreProperties = new HashMap<String, String>();
		content = new HashMap<String, InputStream>();
		zip = new ZipOutputStream(out);
	}
	
	public void addDefaultType(String contentType, String extension) {
		contentTypes.add(new DefaultContentType(contentType, extension));
	}
	
	public void addOverrideType(String contentType, String partName) {
		contentTypes.add(new OverrideContentType(contentType, partName));
	}
	
	public void addRelationship(String id, String target, String type) {
		relationships.add(new Relationship(id, target, type));
	}
	
	public void addCoreProperty(String key, String value) {
		coreProperties.put(key, value);
	}
	
	public void addPartRelationship(String partid, String id, String target, String type) {
		List<Relationship> relations = partRelationships.get(partid);
		if(relations == null) {
			relations = new ArrayList<Relationship>();
			partRelationships.put(partid, relations);
		}
		relations.add(new Relationship(id, target, type));
	}
	
	public void addContent(String name, InputStream in) {
		content.put(name, in);
	}
	
	private String createContentTypesXML() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		// root elements
		Document doc = docBuilder.newDocument();
		doc.setXmlVersion("1.0");
		doc.setXmlStandalone(true);
		
		Element types = doc.createElement("Types");
		types.setAttribute("xmlns", "http://schemas.openxmlformats.org/package/2006/content-types");
		doc.appendChild(types);
		
		for(ContentType t : contentTypes) {
			if(t instanceof DefaultContentType) {
				Element def = doc.createElement("Default");
				
				def.setAttribute("ContentType", t.getContentType());
				def.setAttribute("Extension", ((DefaultContentType) t).getExtension());
				
				types.appendChild(def);
			} else if(t instanceof OverrideContentType) {
				Element override = doc.createElement("Override");
				
				override.setAttribute("ContentType", t.getContentType());
				override.setAttribute("PartName", ((OverrideContentType) t).getPartName());
				
				types.appendChild(override);
			}
		}
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StringWriter w = new StringWriter();
		StreamResult result = new StreamResult(w);
 
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);
 
		transformer.transform(source, result);
		
		return w.toString();
	}
	
	private String createRelsXML() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		// root elements
		Document doc = docBuilder.newDocument();
		doc.setXmlStandalone(true);
		doc.setXmlVersion("1.0");
		
		
		Element Relationships = doc.createElement("Relationships");
		doc.appendChild(Relationships);
		
		for(Relationship r : relationships) {
			Element Relationship = doc.createElement("Relationship");
			
			Relationship.setAttribute("Id", r.getId());
			Relationship.setAttribute("Target", r.getTarget());
			Relationship.setAttribute("Type", r.getType());
			
			Relationships.appendChild(Relationship);
		}
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StringWriter w = new StringWriter();
		StreamResult result = new StreamResult(w);
 
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);
 
		transformer.transform(source, result);
		
		return w.toString();
	}
	
	private void writeMetadata() throws IOException, ParserConfigurationException, TransformerException {
		if(!this.content.containsKey("[Content_Types].xml")) {
			zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
			zip.write(createContentTypesXML().getBytes("UTF-8"));
		}
		if(relationships.size() > 0 && !this.content.containsKey("_rels/.rels")) {
			zip.putNextEntry(new ZipEntry("_rels/.rels"));
			zip.write(createRelsXML().getBytes("UTF-8"));
		}
		//TODO
	}
	
	
	private void writeContent() throws IOException {
		for(String name : content.keySet()) {
			zip.putNextEntry(new ZipEntry(name));
			InputStream in = content.get(name);
			for(int i=in.read();i!=-1;i = in.read()) {
				zip.write(i);
			}
			zip.closeEntry();
		}
	}
	
	private void write() throws IOException, ParserConfigurationException, TransformerException {
		writeMetadata();
		writeContent();
	}
	
	@Override
	public void flush() throws IOException {
		zip.flush();
	}
	
	@Override
	public void close() throws IOException {
		try {
			write();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		zip.close();
	}
}
