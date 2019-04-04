package de.tu_berlin.pes.memo.parser.slx

import java.io.File
import java.io.FileOutputStream
import scala.collection.Map
import de.tu_berlin.pes.memo.model.impl.Block
import de.tu_berlin.pes.memo.model.impl.SignalLine
import org.w3c.dom.Node
import de.tu_berlin.pes.memo.model.impl.Model
import scala.collection.immutable.HashMap
import scala.collection.JavaConverters._
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.DocumentBuilder
import javax.xml.transform.TransformerFactory
import javax.xml.transform.Transformer
import javax.xml.transform.dom.DOMSource
import java.io.StringWriter
import javax.xml.transform.stream.StreamResult
import java.io.InputStream
import scala.util.Try

case class Coord(left: Int, top: Int, right: Int, bottom: Int) {
  val coordinates = List(left,top,right,bottom)
  override def toString(): String = coordinates.mkString(", ")
}

class StringInputStream(input: String) extends InputStream {
  var current = 0
  val bytes = input.getBytes()
  override def read(): Int = {
    if(current >= bytes.length) {
      return -1
    }
    val b = bytes(current)
    current = current + 1
    b.toInt
  }
}

/**
 * Transforms a slx file
 * walks all block/signal nodes and change parameters, in this case the layout
 */
class SlxLayoutTransformer(model: Model, infile: File, outfile: File) {
  require(infile != null)
  require(outfile != null)
  val in = new SlxXmlReader(infile)
  in.read()
  val out = new SlxXmlWriter(new FileOutputStream(outfile))
  val blocks = in.blocks
  val signals = in.signals
  val inxml = in.xml
  
  val nodeIdMap = generateNodeIdMap
  val signalBlockMap = generateSignalBlockMap
  
  def generateSignalBlockMap(): Map[(Block,Block),SignalLine] = {
    var map: Map[(Block,Block),SignalLine] = Map()
    for(s <- model.getSignalLines.asScala) {
      val src = s.getSrcBlock
      val dst = s.getDstBlock
      map = map + ( (src, dst) -> s)
    }
    map
  }
  
  def element2xml(root: Element): String = {
    val docFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance();
		val docBuilder: DocumentBuilder = docFactory.newDocumentBuilder();
 
		// root elements
		val doc = docBuilder.newDocument();
		doc.setXmlStandalone(true);
		doc.setXmlVersion("1.0");
		
		val newNode = root.cloneNode(true)
		doc.adoptNode(newNode)
		
		doc.appendChild(newNode)
		
		val transformerFactory: TransformerFactory = TransformerFactory.newInstance();
		val transformer: Transformer = transformerFactory.newTransformer();
		val source: DOMSource = new DOMSource(doc);
		val w: StringWriter = new StringWriter();
		val result: StreamResult = new StreamResult(w);
		
		transformer.transform(source, result);
		
		w.toString()
  }
  
  def generateNodeIdMap(): Map[Int,Node] = {
    var map: Map[Int,Node] = Map()
    for(n <- blocks.getOrElse(List())) {
      val id = n.getAttributes.getNamedItem("SID").getNodeValue.toInt
      map = map + (id -> n)
    }
    map
  }
  
  def node2block(n: Node): Block = {
    val blockName = n.getAttributes().getNamedItem("Name").getNodeValue()
    val block = model.getBlockByPath(blockName, false)
    block
  }
  
  def generateSignalMappingFromSlx(): Map[Node,SignalLine] = {
    var map = Map[Node,SignalLine]()
    val signalz = signals.getOrElse(List())
    
    var blockmap = Map[Int,Block]()
    
    for(n <- blocks.getOrElse(List())) {
      blockmap = blockmap + (n.getAttributes.getNamedItem("SID").getNodeValue.toInt -> node2block(n))
    }
    
    for(n <- signalz) {
      val src = n.getAttributes.getNamedItem("Src").getNodeValue();
      val dst = n.getAttributes.getNamedItem("Dst").getNodeValue();
      
      val srcId = src.split("#").head.toInt
      val dstId = dst.split("#").head.toInt
      
      //map = map + (n -> model.getSignal 
    }
    map
  }
  
  def getChildsByName(n: Node, name: String): Option[List[Node]] = {
    val childs = n.getChildNodes
    var ret: List[Node] = List()
    for(i <- 0 to childs.getLength-1) {
      val child = childs.item(i)
      if(child != null && child.getNodeType == Node.ELEMENT_NODE) {
        val elem = child.asInstanceOf[Element]
        if(elem.getNodeName == name) {
          ret = child :: ret
        }
      }
    }
    if(ret.length == 0) {
      return None
    }
    return Some(ret)
  }
  
  def getParam(n: Node, name: String): Option[String] = {
    val childs = n.getChildNodes
    for(i <- 0 to childs.getLength-1) {
      val child = childs.item(i)
      if(child.getNodeType == Node.ELEMENT_NODE) {
        val elem = child.asInstanceOf[Element]
        if(elem.getAttribute("Name") == name) {
          return Some(elem.getTextContent)
        }
      }
    }
    return None
  }
  
  def branch2signal(branch: Node, src: String): List[SignalLine] = {
    var signals: List[SignalLine] = List()
    val branches = getChildsByName(branch, "Branch")
    if (branches != None) {
      for (b <- branches.get) {
        signals = signals ++ branch2signal(b, src)
      }
    }
    val dstoption = getParam(branch,"Dst")
    
    if (dstoption.isDefined) {
      val dst = dstoption.get
      val srcNodeId = src.split("#").head.toInt
      val dstNodeId = dst.split("#").head.toInt
      
      val srcNode = nodeIdMap.get(srcNodeId).get
      val dstNode = nodeIdMap.get(dstNodeId).get
      
      val srcBlock = node2block(srcNode)
      val dstBlock = node2block(dstNode)
      
      val signal = signalBlockMap.get((srcBlock,dstBlock))
      if(signal.isDefined) {
        signals = signal.get :: signals
      }
    }
    signals
  }
  
  /**
   * gets the signalline from a node
   * 1. get node <-> node map for the signal
   * 2. use node2block to convert to block <-> block mapping
   * 3. resolve block <-> block mapping to signallines
   */
  def node2signal(n: Node): List[SignalLine] = {
    // TODO add branched lines
    val src = getParam(n,"Src").get
    val dst = getParam(n,"Dst")
    var signals: List[SignalLine] = List()
    if(!dst.isDefined) {
      // we got a branch
      val branches = getChildsByName(n, "Branch")
      println(branches)
      for(b <- branches.get) {
        signals = signals ++ branch2signal(b, src)
      }
    } else {
      val srcNodeId = src.split("#").head.toInt
      
      val dstNodeId = dst.get.split("#").head.toInt
      val srcNode = nodeIdMap.get(srcNodeId).get
      val dstNode = nodeIdMap.get(dstNodeId).get
      
      val srcBlock = node2block(srcNode)
      val dstBlock = node2block(dstNode)
      
      val signal = signalBlockMap.get((srcBlock,dstBlock))
      if(signal != None) {
        signals = signal.get :: signals
      }
    }
    
    signals
  }
  
  def transform(blockmap: java.util.HashMap[Block,Block], signalmap: java.util.HashMap[SignalLine,SignalLine]): Unit = {
    transform(blockmap.asScala,signalmap.asScala)
  }
  
  /**
   * uses the blocks to change all block elements in the doc
   */
  def changeBlocks(doc: Element) = {
    // TODO
  }
  
  /**
   * uses the signals to change all signal elements in the doc
   */
  def changeSignals(doc: Element) = {
    // TODO
  }
  
  def changeBlockPosition(xml: Element, block: Block, pos: Coord): Unit = {
    val nodelist = xml.getChildNodes.item(1).getChildNodes
    for(i <- 0 to nodelist.getLength-1) {
      val item = nodelist.item(i)
      if(item.getNodeType == Node.ELEMENT_NODE) {
        val elem = item.asInstanceOf[Element]
        if(elem.getNodeName == "System") {
          val nodelist = elem.getChildNodes
          for(i <- 0 to nodelist.getLength-1) {
            val item = nodelist.item(i)
            if(item.getNodeType == Node.ELEMENT_NODE) {
              val elem = item.asInstanceOf[Element]
              if(elem.getNodeName == "Block" && elem.getAttribute("Name") == block.getName) {
                val nodelist = elem.getChildNodes
                for(i <- 0 to nodelist.getLength-1) {
                  val item = nodelist.item(i)
                  if(item.getNodeType == Node.ELEMENT_NODE) {
                    val elem = item.asInstanceOf[Element]
                    if(elem.getNodeName == "P") {
                      if(elem.getAttribute("Name") == "Position") {
                        elem.setTextContent("["+pos.toString()+"]")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  
  def blockPosition2coords(position: String): Coord = {
    val positions = position.split(" ").map(x => x.replaceAll("\\[|\\]| ", "")).filter(s => !s.isEmpty())
    val left = positions(0).toFloat.toInt
    val top = positions(1).toFloat.toInt
    val right = positions(2).toFloat.toInt
    val bottom = positions(3).toFloat.toInt
    new Coord(left,top,right,bottom)
  }
  
  def transform(blockmap: Map[Block,Block], signalmap: Map[SignalLine,SignalLine]): Unit = {
    var xml = in.xml
    
    // transfrom the xml element below
    
    val blockz = blocks.getOrElse(List())
    for(n <- blockz) {
      val b = node2block(n)
      val newBlock = blockmap.get(b)
      // TODO: transform block node b
      val newb = n.cloneNode(true)
      if(newBlock != None) {
        changeBlockPosition(xml, newBlock.get, blockPosition2coords(newBlock.get.getParameter("Position"))) 
      }
    }
    val signalz = signals.getOrElse(List())
    for(n <- signalz) {
      for(s <- node2signal(n)) {
        val newSignal = signalmap.get(s)
        // TODO: transform signal node s
        val news = n.cloneNode(true)
      }
    }
    
    out.addContent("simulink/blockdiagram.xml", new StringInputStream(element2xml(xml)))
    write()
  }
  
  private def write(): Unit = {
    for((k,v) <- in.getContent.asScala) {
        if(k != "simulink/blockdiagram.xml") {
          out.addContent(k, v)
        }
    }
    // TODO
    out.flush()
    out.close()
  }
}