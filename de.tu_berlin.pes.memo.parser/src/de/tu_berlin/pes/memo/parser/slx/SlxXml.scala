package de.tu_berlin.pes.memo.parser.slx

import de.tu_berlin.pes.memo.parser.slx.opc.OPCReader;
import java.io.InputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.w3c.dom.Node
import de.tu_berlin.pes.memo.parser.slx.opc.OPCWriter
import java.io.OutputStream

/*
 * Tries to parse the SLX format without sacrificing orginal file content
 * aka everything that is parsed can be written back exactly as it was parsed
 */
class SlxXmlReader(file: File) extends OPCReader(file) {
  this.read()
  val xml: Element = {
    val factory = DocumentBuilderFactory.newInstance();
		val builder = factory.newDocumentBuilder();
		val doc = builder.parse(this.getContent().get("simulink/blockdiagram.xml"));
		doc.getDocumentElement()
  }
  
  def nodeList2Scala(list: NodeList): List[Node] = {
    var scalaList: List[Node] = List()
    for( i <- 0 to list.getLength()) {
      scalaList = list.item(i) :: scalaList
    }
    scalaList.reverse
  }
  
  def model = nodeList2Scala(xml.getChildNodes).find { x => x.isInstanceOf[Element] && x.getNodeName() == "Model" }
  
  def system = model.flatMap {x => nodeList2Scala(x.getChildNodes).find { x => x.isInstanceOf[Element] && x.getNodeName() == "System" } }
  def blocks = system.flatMap {x => Some(nodeList2Scala(x.getChildNodes).filter { x => x.isInstanceOf[Element] && x.getNodeName() == "Block" }) }
  def signals = system.flatMap {x => Some(nodeList2Scala(x.getChildNodes).filter { x => x.isInstanceOf[Element] && x.getNodeName() == "Line" }) }
}

class SlxXmlWriter(out: OutputStream) extends OPCWriter(out) {
  
}