package de.tu_berlin.pes.memo.parser.slx;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.tu_berlin.pes.memo.parser.slx.opc.OPCReader;
import de.tu_berlin.pes.memo.graph_embedding.common.LayeredGraphTransformer;
import de.tu_berlin.pes.memo.graph_embedding.config.GraphConfig;
import de.tu_berlin.pes.memo.graph_embedding.model.Graph;
import de.tu_berlin.pes.memo.graph_embedding.model.Node;
import de.tu_berlin.pes.memo.graph_embedding.model.edges.Edge;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.parser.persistence.MeMoPersistenceManager;
import de.tu_berlin.pes.memo.parser.slx.SlxLayoutTransformer;
import de.tu_berlin.pes.memo.model.impl.Block;

public class TestOPCReader {
	public static void main(String[] args) throws ZipException, IOException, ParserConfigurationException, SAXException {
		OPCReader reader = new OPCReader(new File("/home/moritz/Schreibtisch/annotation_only.slx"));
		reader.read();
		System.out.println("Content");
		System.out.println(reader.getContent());
		System.out.println("ContentTypes");
		System.out.println(reader.getContentTypes());
		System.out.println("CoreProperties");
		System.out.println(reader.getCoreProperties());
		System.out.println("PartRelationships");
		System.out.println(reader.getPartRelationships());
		System.out.println("Relationships");
		System.out.println(reader.getRelationships());
		
		reader.close();
		
		SlxXmlReader slxparser = new SlxXmlReader(new File("/home/moritz/Schreibtisch/cismo-root/trunk/misc/simulink_test_models/f14/f14.slx"));
		System.out.println(slxparser.blocks());
		System.out.println(slxparser.model());
		System.out.println(slxparser.signals());
		System.out.println(slxparser.system());
		
		MeMoPersistenceManager persistence = MeMoPersistenceManager.getInstance();
		Model m = persistence.getModel();
		
		de.tu_berlin.pes.memo.graph_embedding.common.LayeredGraphTransformer lgt = new LayeredGraphTransformer();
		GraphConfig gc = new GraphConfig();
		Graph g = lgt.transform(m, gc);
		HashMap<Block,Block> blockMapping = new HashMap<>();
		for(Node n : g.nodes) {
			if(n.origin instanceof Block) {
				Block newBlock = (Block) n.origin;
				Block oldBlock = m.getBlockByPath(newBlock.getFullQualifiedName(false),false);
				blockMapping.put(oldBlock, newBlock);
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
		
		SlxLayoutTransformer transform = new SlxLayoutTransformer(m,new File("/home/moritz/Schreibtisch/cismo-root/trunk/misc/simulink_test_models/f14.slx"), new File("/home/moritz/Schreibtisch/f14_transformed.slx"));
		
		System.out.println(transform.blocks());
		
	}
}
