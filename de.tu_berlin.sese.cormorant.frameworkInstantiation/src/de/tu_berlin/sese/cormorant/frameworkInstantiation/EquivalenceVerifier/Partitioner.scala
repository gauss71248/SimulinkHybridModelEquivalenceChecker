package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier

//trait PrettyPrint {
//  def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String
//}


/**
 * A partition is a set of vertices. It keeps its inner structure. A partitioned graph, i.e., a Partitioning, can be seen as a graph of higher order. Its vertices, which are the partitions, are a graph as well.
 */
case class Partition(val graph: SimulinkGraph, val elements: Set[SimulinkVertex], val solved: Boolean) {
  /**
   * The function merges two partitions.
   */
  def ++(p: Partition): Partition = new Partition(p.graph, elements ++ p.elements, false)
  
  def ++(s: Set[SimulinkVertex]): Partition = new Partition(graph, elements ++ s, false)
  /**
   * The function adds a single vertex to a partition. The graph is also updated.
   */
  def +(g: SimulinkGraph, v: SimulinkVertex): Partition = new Partition(g, elements ++ Set(v), false)
  /**
   * The function provides the graph consisting only of elements and edges of the partition.
   */
  def partitionGraphRestriction: SimulinkGraph = graph.graphRestriction(elements)
  /**
   * The function tells if a given Simulink Vertex is part of the partition.
   */
  def contains(v: SimulinkVertex): Boolean = elements.contains(v)
  /**
   * The function tells if a partition is a super-partition of this partition. The symbol <= reminds on the subset relation.
   */
  def <=(p: Partition): Boolean = elements.foldLeft(true)((b,v)=>b&& p.contains(v))
  
  /**
   * The function tells if the subgraph determined by the partition contains a cycle.
   */
  def containsCycle: Boolean = !graph.graphRestriction(elements).isDAG || elements.head.isInstanceOf[TransferFunctionVertex]||this.solved
  
  override def toString: String = "Partition consisting of "+elements
  
//  def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String = {
//    var output:String = ""
//    val edges = graph.edges.filter(e => elements.contains(e.source) && elements.contains(e.target))
//    for( v <- elements) {
//      output += output + "\"" + v.pretty(graph, visited) + "\";\n"
//    }
//    for( e <- edges) {
//      output += "\"" + e.source.pretty(graph, visited) + "\" -> \"" + e.target.pretty(graph, visited) + "\";\n"
//      
//    }
//    
//    output += "}\n"
//    output
//  }
  
  def graphviz: String = graph.graphRestriction(elements).graphviz
  
  
  
  def tgf: String = elements.foldLeft("")((s,e)=> s+ e + ";").replaceAll("#", " ")
  
  def isStateLess: Boolean = partitionGraphRestriction.isStateLess
  
  def isPurelyTimeContinuous: Boolean = partitionGraphRestriction.isPurelyTimeContinuous
}

//object Partition {
//     implicit def vertex2Partition(v: SimulinkVertex, g: SimulinkGraph): Partition = new Partition(g, Set(v))
//}
/**
 * A Partition Connection plays the role of edges of a graph in the higher order Partitioning. It directly connects two partitions. A Partition Connection between the partitions s and t is set up
 * if and only if a block b1 exists in s and a block b2 exists in t such that (b1,b2) are connected in the Simulink graph. Self-referring connections are ignored.
 */
case class PartitionConnection(val source: Partition, val target: Partition) {
  override def toString: String = "Partition Connection:" + source + "---->" + target
}

/**
 * A partitioning is a higher order graph. Its vertices are partitions, i.e., sets of vertices from the Simulink graph. Its edges are PartitionConnections. 
 */
case class Partitioning(val graph: SimulinkGraph, val partitions: Set[Partition], val connections: Set[PartitionConnection])  {
   import Partitioning._
   
   
//   def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String = {
//     var output:String = "digraph Partitions {\n"
//     var i = 0
//     var outside_edges_added: List[SimulinkEdge] = List()
//     for( p <- partitions) {
//       output += "subgraph cluster_"+i+" {\n"
//       output += "style=filled;\n"
//		   output += "color=lightgrey;\n"
//		   output += "label = \"partition_"+i+"\";"
//       output += p.pretty(graph, visited)
//       
//       // output edges
//       val all_other_partitions = partitions.filter(_ != p)
//       val all_other_partition_vertices = all_other_partitions.map(_.elements).foldLeft(Set(): Set[SimulinkVertex])((a,b) => a ++ b)
//       
//       val outside_edges = graph.edges.filter(e => (p.elements.contains(e.source) || p.elements.contains(e.target)) &&
//                                                  !(p.elements.contains(e.source) && p.elements.contains(e.target)))
//              .filter(e => !outside_edges_added.contains(e))
//      
//       for(e <- outside_edges) {
//         output += "\"" + e.source.pretty(graph, visited) + "\" -> \"" + e.target.pretty(graph, visited) + "\";\n"
//         outside_edges_added = e :: outside_edges_added
//       }
//       i = i + 1
//     }
//     output += "}\n"
//     output
//   }
   
   def graphviz: String = {
     val parString = (partitions zip Stream.from(0)).map(x => x._1.solved match {
       case false => "subgraph cluster_"+x._2+" {\n style=filled;\n color=lightgrey;\n label = \"partition_"+x._2+"\";" + graph.graphRestriction(x._1.elements).graphviz + "}"
       case true => "subgraph cluster_"+x._2+" {\n style=filled;\n color=green;\n label = \"partition_"+x._2+"\";" + graph.graphRestriction(x._1.elements).graphviz + "}"
     })
     val crossingString = crossingEdges.map(e=>e.graphviz)
     val interfaceInputEdges = graph.vertices.filter(v=>v.isInstanceOf[InterfaceInputVertex]).foldLeft("")((s,v) => s+ "\"" + v.asInstanceOf[InterfaceInputVertex].predblock.graphviz + "\" -> \"" + v.graphviz + "\" [style=dotted];\n" )
     val interfaceOutputEdges = graph.vertices.filter(v=>v.isInstanceOf[InterfaceOutputVertex]).foldLeft("")((s,v) => s+ "\"" + v.graphviz + "\" -> \"" + v.asInstanceOf[InterfaceOutputVertex].succblock.graphviz + "\" [style=dotted];\n" )
     "digraph Partitions {\n" + parString.foldLeft("")((s,p)=> s+p) + crossingString.foldLeft("")((s,e)=> s+e) + interfaceInputEdges + interfaceOutputEdges + "}\n"
     
    
   }
    /**
    * This function provides a Partition containing a given Simulink Vertex.
    */
   def partition4Vertex(v: SimulinkVertex): Option[Partition] = partitions.find { p => p.contains(v) }
   def crossingEdges: Set[SimulinkEdge] = graph.edges.filter(e=> (partition4Vertex(e.source)!=partition4Vertex(e.target)))
  


   /**
    * The function merges two partitions. Both partitions are removed from the partitioning, the merged partition is added and the connections are re-calculated. This
    * yields a new Partitioning with the same connections as the original Partitioning except for the connections within the partitions that were merged.
    * Self-referring connections are removed.
    */
   def ++(p1: Partition, p2: Partition): Partitioning = {
    val newPartitions = partitions - p1 - p2 + (p1++p2)
    val newConnsFull = obtainAllRequiredConnections(graph, newPartitions.toList).toSet 
    val newConns = newConnsFull -- newConnsFull.filter(c => c.source==c.target)
    new Partitioning(graph, newPartitions, newConns)
  }
  
    /**
     * The function tells if a given Simulink Vertex is within a partition of the Partitioning.
     */
   def isVertexInPartition(v: SimulinkVertex): Boolean = partitions.foldLeft(false)((b,p)=>b || p.contains(v))
   /** 
    *  A path is a list, i.e., a sequence of partitions, i.e., vertices in the Partitioning graph.
    */
   type Path = List[Partition]
   /**
    * The function tells if a list of partitions, i.e., vertices in the Partitioning, is a path. A path is a sequence, i.e., a list of vertices where subsequent
    * vertices, i.e., partitions are connected.
    */
   def isPath(pars: Path): Boolean = pars match {
     case Nil => true
     case List(p) => true
     case (p1::p2::ps) if connections.contains(new PartitionConnection(p1,p2)) => isPath(ps)
     case _ => false
   }
   /**
    * The function tells if a path between two given partitions exists in the Partitioning.
    */
   def existsPath(p1: Partition, p2: Partition): Boolean = existsPath(p1,p2,Nil.toSet)
   /**
    * This is a helping function for existsPath. It uses a working set to avoid unlimited runs in cycles during the recursion.
    */
   private def existsPath(p1: Partition, p2: Partition, working: Set[Partition]): Boolean = p1==p2 match {
     case true => true
     
     case false if !successors(p1).foldLeft(true)((b,v) => b && working.contains(v)) => successors(p1).filter(x => !working.contains(x)) match {
       case Nil => false
       case s => s.foldLeft(false)((b,p) => b || existsPath(p,p2,working + p))
     }
     case _ => false
   }
   
   
   /**
    * This function finds all paths between two given partitions.
    */
   def findPaths(p1:Partition, p2:Partition): List[Path] = existsPath(p1,p2) match {
     case false => Nil
     case true => p1==p2 match {
       case true => List(List(p1))
       case false => {
         val succs = successors(p1).filter(p=> existsPath(p,p2))
         succs.map(x=>p1::this.findPaths(x, p2).flatten)
       }
     }
   }
   
   
//   //falls noch nicht Fusion Complete, ähnlich wie findCyclicComponent
//   def findDAGComponents2Connect(l: List[Partition]): Option[(Partition, Partition)] = {
//     val relevantPartitions: Set[Partition] = partitions.filter(p=>p.elements.size==1)
//     
//   }
//   //paarweise checken, ob Folgendes gilt: 
//   def isDAGComponentFusionComplete: Boolean = {
//     val relevantPartitions = partitions.filter(p=>p.elements.size==1)
//     
//   }
//  
   /**
    * The function was meant as an auxiliary function for the connection of DAG chains. However, we decided to pursue a different approach. The function remains here nevertheless.
    *  The idea is to connect paths of partitions that are singletons, i.e., do not contain cyclic parts. A path is considered to be pure if it does not contain
    *  cyclic partitions. Cyclic partitions are recognised if they contain more than one element.  
    */
   private def isPathPurelyDAG(p: Path): Boolean = p.foldLeft(true)((b,v)=> b && v.elements.size==1)
   /**
    * The function tells if a direct connection from the first argument to the second exists or if the partitions, i.e., vertices are equal.
    */
   def isConnected(p1: Partition, p2: Partition): Boolean = p1==p2 || (connections.find { x => new PartitionConnection(p1,p2) == x } match {
     
     case Some(v) => true
     case _ => false
   })
   /**
    * The function tells if two partitions are equivalent, i.e., in relation with respect to the following equivalence relation:
    * p1=p2 if and only if a path exists from p1 to p2 and vice versa
    */
   def equivalent(p1: Partition, p2: Partition): Boolean = existsPath(p1,p2) && existsPath(p2,p1)
   
   /**
    * The function provides a list of partitions that directly succeed the given partition in the partitioning.
    */
   def successors(p: Partition): List[Partition] = connections.filter { x => x.source==p }.map { x => x.target }.toList
   /**
    * The function provides the direct predecessors of a given partition in the partitioning.
    */
  def predecessors(p: Partition): List[Partition] = connections.filter { x => x.target==p }.map { x => x.source }.toList
  
  /**
   * The function provides the direct predecessors of a given partition in the partitioning fulfilling a predicate f.
   */
  def predecessors(p: Partition, f: Partition => Boolean): List[Partition] = connections.filter { x => x.target==p }.filter(x => f(x.source)).map { x => x.source }.toList
  /**
   * The function provides the partition that contains the given partition, i.e., which is a superset of the given partition. It is used as auxiliary function to
   * connect the DAG (directed acyclic graph), i.e., non-cyclic partitions. 
   */
  def surroundingPartition(p: Partition): Option[Partition] = partitions.find(x=>p<=x)
  
 /**
  * The function is an auxiliary function for the isDAG function. It defines the recursion.
  */
 private def isDAG(l: List[Partition]): Boolean = l match {
    case Nil => true  
    case (p::ps) => partitions.foldLeft(true)((b,v)=> b && ((p==v) || !equivalent(p,v))) && isDAG(ps)
  }
  /**
  * The function tells if the partitioning is acyclic. It is the terminating case to calculate the second step of the partitioning, the merging of cyclic elements.
  */
  def isDAG: Boolean = isDAG(partitions.toList)
  /**
   * The function identifies two partitions (if existent) that are equivalent. These are member of the same cycle. They are consequently candidates for merging during the
   * second phase of the partitioning: the merging of cyclic components.
   */
  def findCyclicComponent: Option[(Partition, Partition)] = findCyclicComponent(partitions.toList)
  /**
   * The function is an auxiliary function for the findCyclicComponents function. It defines the recursion.
   */
  private def findCyclicComponent(l: List[Partition]): Option[(Partition,Partition)] = isDAG match {
    case true => None
    case false => l match {
      case Nil => None
      case (p::ps) => ps.find(x=> equivalent(p,x)) match {
        case Some(y) => Some((p,y))
        case None => findCyclicComponent(ps)
      }
    }
  }
  /**
   * This is an auxiliary function for the third phase of the partitioning: the merging of the DAG parts. It tells if a singleton predecessor exists for a given partition 
   * in the partitioning. It is the terminating case for the recursion to calculate the DAG parts. 
   */
  def existsSingletonPredecessor(p: Partition): Boolean = predecessors(p).foldLeft(false)((b,v)=> b|| !p.containsCycle)
   override def toString: String = "Partition: " + partitions + "||||" + connections
  
  // def isTransitive(x: PartitionConnection): Boolean = connections.filter(c=>c.source==x).map(a=>a.target)
   
   //def transitiveHull: Set[PartitionConnection] = 

}

object Partitioning {
  
  
  /**
   * The function tells if a connection from one partition to another is required. It is required if and only if a vertex in the graph in the first partition exixts 
   * which is connected to a vertex in the second partition in the graph. The function is an auxiliary function to calculate the new connections if partitions are merged within
   * a partitioning.
   */
  def connectionRequired(graph: SimulinkGraph, source: Partition, target: Partition): Boolean = source.elements.foldLeft(false)((b1,s)=>(b1||target.elements.foldLeft(false)((b2,t)=> b2 ||graph.containsConnection(s, t))))
  /**
   * The function provides the required connections from a source partition to a list of partitions. The function is an auxiliary function to calculate the new connections if partitions are merged within
   * a partitioning.
   */
  def obtainRequiredConnections(graph: SimulinkGraph, source: Partition, target: List[Partition]): List[PartitionConnection] = target match{
   
    case (p::ps) if connectionRequired(graph,source,p) => new PartitionConnection(source,p)::obtainRequiredConnections(graph, source, ps)
    case (p::ps) if connectionRequired(graph, p, source) => new PartitionConnection(p,source)::obtainRequiredConnections(graph, source, ps)
    case (p::ps) => obtainRequiredConnections(graph, source, ps)
    case Nil => Nil
       
  }
  /**
   * The function provides all required connections for a list of partitions. The function is an auxiliary function to calculate the new connections if partitions are merged within
   * a partitioning.
   */
  def obtainAllRequiredConnections(graph: SimulinkGraph, partitions: List[Partition]): List[PartitionConnection] = partitions.map { x => obtainRequiredConnections(graph, x,partitions) }.flatten 

   /**
    * This function is the first step of partitioning. Besides the starting graph, it gets a set of vertices. For each of these in the list the following is performed:
    * All preceeding vertices including the vertex itself are merged to a partition. The idea is to give the user the opportunity to identify submodels that are solved in the
    * target model. For instance, a sine wave generator can be expressed by an ODE. Both yield different submodels. The user will put the last block of the submodel, i.e., the block
    * providing the same output in source and target model in the list. This puts them in a partition in both models.
    * All other vertices are put in singleton partitions.
    */
   def prePartitioning(graph: SimulinkGraph, conns: Set[SimulinkVertex]): Partitioning = {
    val pres: Set[Partition] = conns.map { x => new Partition(graph, graph.predecessors(x, Nil.toSet) +x, true) }
    val others: Set[Partition] = graph.vertices.withFilter { x => !pres.foldLeft(false)((b,p)=>p.contains(x)) }.map { x => new Partition(graph,Set(x), false) } 
    val pars: Set[Partition] = pres ++ others
    val connections = obtainAllRequiredConnections(graph,pars.toList).toSet
    new Partitioning(graph, pars, connections)
  }
  
 
  /**
   * This function is the second step of partitioning: The merging of cyclic parts. Non-cyclic parts remain in singleton partitions.
   */
  def equivalenceClassesPartitioning(par: Partitioning): Partitioning = par.isDAG match {
    case true => par
    case false => par.findCyclicComponent match {
      case Some((p1,p2)) => equivalenceClassesPartitioning(par.++(p1,p2))
      case None => par
    }
  }
/**
 * The function is the third part of the partitioning process. It merges connected, non-cyclic, i.e., non-singleton partitions together. It starts from output vertices and
 * cyclic vertices and moves backwards in the partitioning collecting all non-cyclic elements.
 */
  def connectDAGParts(par: Partitioning): Partitioning =  {
    val outputs = par.partitions.filter(p => p.elements.head.isInstanceOf[OutputVertex])
    //val cycles = par.partitions.filter(p => p.elements.size>1)
    val cycles = par.partitions.filter(p=>p.containsCycle)
    val cyclePres = cycles.foldLeft(Set(): Set[Partition])((s,c)=>s ++ par.predecessors(c))
    val outputsConnected = outputs.foldLeft(par)((part,p)=> connectDAGParts(part,p))
    //cycles.foldLeft(outputsConnected)((part,p)=> connectDAGParts(part,p))
    cyclePres.foldLeft(outputsConnected)((part,p)=> connectDAGParts(part,p))
  }
   import de.tu_berlin.pes.memo.MeMoPlugin
   /**
    * The function is an auxiliary function to the connectDAGParts function and defined the recursion.
    */
  def connectDAGParts(par: Partitioning, p: Partition): Partitioning = par.existsSingletonPredecessor(p) match {
    case false => par
    case true => {
     // par.predecessors(p).filter(x=>x.elements.size==1).foldLeft(par)((x,y) => x.surroundingPartition(p) match {
      par.predecessors(p, x => !x.containsCycle).foldLeft(par)((x,y) => x.surroundingPartition(p) match {
        case Some(z)=>{
          val temp = x.++(z,y)
          
          temp.surroundingPartition(y) match {
            case Some(a) => connectDAGParts(temp,a)
            case _ => temp
          }
        }
        case _ => x 
      })
   }
  }
//   /**
//    * The implicit function transforms a graph into a Partitioning by packing each vertex into a singelton partition
//    */
//   implicit def graph2Partitioning(g: SimulinkGraph): Partitioning = {
//     
//    val partitions: Set[Partition] = g.vertices.map { x => new Partition(g,Set(x)) }
//    val connections = obtainAllRequiredConnections(g, partitions.toList).toSet
//    new Partitioning(g, partitions, connections)
//   }
   


//   /**
//    * adding a pair of virtual vertices for every crossingEdge between partitions, the virtual blocks are getting connected to the target/source of the crossingEdge and added to the elements set
//    * of the corresponding partition 
//    */
//   def addInterfaceVertices(par: Partitioning): Partitioning = {
//
//     val newVertices = par.graph.vertices
//     val newEdges = par.graph.edges
//     
//     for(crossingEdge <- par.crossingEdges){
//       val input  = new InterfaceInputVertex(par.graph.maxID + 1, None)
//       val partitionInputEdge = new SimulinkEdge(input, crossingEdge.target, crossingEdge.arity)
//       par.partition4Vertex(crossingEdge.target).get.elements + input //adding virtual input block to the set of elements of the partition it shall belong to
//       
//       
//       val output = new InterfaceOutputVertex(par.graph.maxID + 1, None)
//       val partitionOutputEdge = new SimulinkEdge(crossingEdge.source, output, crossingEdge.arity)
//       par.partition4Vertex(crossingEdge.source).get.elements + output //adding virtual output block to the set of elements of the partition it shall belong to
//
//       
//       newVertices + input + output
//     }
//     
//     new Partitioning(new SimulinkGraph(newVertices, newEdges), par.partitions, par.connections) 
//   }
//   
//   
//   
   
  
}

object EnrichedPartitioning {
  
  import Partitioning._
  def addInterfaces(par: Partitioning, p: Partition): Partitioning = {
    val subgraph: SimulinkGraph = par.graph.graphRestriction(p.elements)
    val inputMisses: Set[SimulinkVertex] = (par.graph.edges -- subgraph.edges).filter(e => subgraph.vertices.contains(e.target)).map(e => e.target)
    val interfaceInputEdges2Add: Set[SimulinkEdge] = inputMisses.foldLeft(Set(): Set[SimulinkEdge])((s,v) => s ++ unmatchedArities(v,par.graph, subgraph).foldLeft(Set(): Set[SimulinkEdge])((s2,x)=> s2 + new SimulinkEdge(new InterfaceInputVertex(par.graph.maxID, x._1), v,x._2))) 
    val interfaceInputVertices2Add: Set[SimulinkVertex] = interfaceInputEdges2Add.map(e => e.source)
    val outputMisses: Set[SimulinkEdge] = (par.graph.edges -- subgraph.edges).filter(e => subgraph.vertices.contains(e.source))
    val interfaceOutputEdges2Add: Set[SimulinkEdge] = outputMisses.foldLeft(Set(): Set[SimulinkEdge])((s,v)=> s + new SimulinkEdge(v.source, new InterfaceOutputVertex(par.graph.maxID +1, v.target, v.arity),1))
    val interfaceOutputVertices2Add: Set[SimulinkVertex] = interfaceOutputEdges2Add.map(e => e.target)
    val newgraph: SimulinkGraph = new SimulinkGraph(par.graph.vertices ++ interfaceInputVertices2Add ++ interfaceOutputVertices2Add, par.graph.edges ++ interfaceInputEdges2Add ++ interfaceOutputEdges2Add)
    val newPartition: Partition = new Partition(newgraph,p.elements ++ interfaceInputVertices2Add ++ interfaceOutputVertices2Add, p.solved)
    val newConns = obtainAllRequiredConnections(newgraph, (par.partitions - p + newPartition).toList).toSet 
    new Partitioning(newgraph,par.partitions - p + newPartition, newConns--newConns.filter(c => c.source==c.target))
  }
   def addAllInterfaces(par: Partitioning): Partitioning = equaliseGraphInformation(par.partitions.foldLeft(par)((pa, p) => addInterfaces(pa,p)))
   private def equaliseGraphInformation(par: Partitioning): Partitioning = {
     val graph = par.graph
     val updatedPartitions = par.partitions.map(p=> new Partition(graph,p.elements, p.solved))
     new Partitioning(graph,updatedPartitions,par.connections)
   }
   def unmatchedArities(v: SimulinkVertex, graph: SimulinkGraph, subgraph: SimulinkGraph): Set[(SimulinkVertex, Int)] = (graph.edges -- subgraph.edges).filter(e => e.target == v).map(e => (e.source, e.arity))
    
}


