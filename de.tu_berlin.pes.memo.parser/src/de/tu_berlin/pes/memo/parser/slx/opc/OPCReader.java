package de.tu_berlin.pes.memo.parser.slx.opc;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class implements a generic ISO/IEC 29500:2008 and ECMA-376
 * Open Packaging Conventions package reader
 * @author moritz
 *
 */
public class OPCReader implements Closeable, AutoCloseable{
	public enum Type {
		DEFAULT, OVERRIDE
	};
	
	private List<ContentType> contentTypes;
	private List<Relationship> relationships;
	private Map<String, List<Relationship>> partRelationships;
	
	private Map<String,InputStream> content;
	private Map<String, String> coreProperties;
	
	private ZipFile zip;
	
	public OPCReader(File file) throws ZipException, IOException {
		contentTypes = new ArrayList<ContentType>();
		relationships = new ArrayList<Relationship>();
		partRelationships = new HashMap<String, List<Relationship>>();
		coreProperties = new HashMap<String, String>();
		content = new HashMap<String, InputStream>();
		zip = new ZipFile(file);
	}
	
	public List<ContentType> getContentTypes() {
		return contentTypes;
	}

	public List<Relationship> getRelationships() {
		return relationships;
	}

	public Map<String, List<Relationship>> getPartRelationships() {
		return partRelationships;
	}

	public Map<String, InputStream> getContent() {
		return content;
	}

	public Map<String, String> getCoreProperties() {
		return coreProperties;
	}

	private void addDefaultType(String contentType, String extension) {
		contentTypes.add(new DefaultContentType(contentType, extension));
	}
	
	private void addOverrideType(String contentType, String partName) {
		contentTypes.add(new OverrideContentType(contentType, partName));
	}
	
	private void addRelationship(String id, String target, String type) {
		relationships.add(new Relationship(id, target, type));
	}
	
	private void addCoreProperty(String key, String value) {
		coreProperties.put(key, value);
	}
	
	private void addPartRelationship(String partid, String id, String target, String type) {
		List<Relationship> relations = partRelationships.get(partid);
		if(relations == null) {
			relations = new ArrayList<Relationship>();
			partRelationships.put(partid, relations);
		}
		relations.add(new Relationship(id, target, type));
	}
	
	private void addContent(String name, InputStream in) {
		content.put(name, in);
	}
	
	private void readContentTypesXML(ZipEntry entry) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(zip.getInputStream(entry));
		Element types = doc.getDocumentElement();
		
		NodeList l = types.getChildNodes();

		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if(e.getNodeName().equals("Default")) {
					String ct = e.getAttribute("ContentType");
					String ext = e.getAttribute("Extension");
					//System.out.println(ct);
					//System.out.println(ext);
					addDefaultType(ct, ext);
				} else if(e.getNodeName().equals("Override")) {
					String ct = e.getAttribute("ContentType");
					String partName = e.getAttribute("PartName");
					//System.out.println(ct);
					//System.out.println(partName);
					addOverrideType(ct, partName);
				}
			}
			
		}
	}
	
	private void readRelsXML(ZipEntry entry) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(zip.getInputStream(entry));
		Element rels = doc.getDocumentElement();
		
		NodeList l = rels.getChildNodes();
		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if(e.getNodeName().equals("Relationship")) {
					String id = e.getAttribute("Id");
					String target = e.getAttribute("Target");
					String type = e.getAttribute("Type");
					this.addRelationship(id, target, type);
				}
			}
		}
	}
	
	private void readMetadata() throws ParserConfigurationException, SAXException, IOException {
		readContentTypesXML(zip.getEntry("[Content_Types].xml"));
		readRelsXML(zip.getEntry("_rels/.rels"));
	}
	
	private void readContent() throws IOException {
		Enumeration<? extends ZipEntry> e = zip.entries();
		while(e.hasMoreElements()) {
			ZipEntry ze = e.nextElement();
			addContent(ze.getName(), zip.getInputStream(ze));
		}
	}
	
	public void read() throws IOException, ParserConfigurationException, SAXException {
		readMetadata();
		readContent();
	}
	
	@Override
	public void close() throws IOException {
		zip.close();
	}
}
