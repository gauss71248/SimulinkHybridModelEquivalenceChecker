

package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier
import de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier.SimulinkGraph._



class DataFlowModel(val models: Set[SimulinkGraph], val configuration: SimulinkGraph => List[Boolean]) {
  
  type Configuration = List[Boolean]
  
}
class DataFlowModelCalculator(val graph: SimulinkGraph) {
  /**
   * dataFlowModels are a set of pure models, i.e., without control flow elements.
   */
  def dataFlowModels: Set[SimulinkGraph] = calculateDataFlowModels(List(graph)).toSet
 /**
  * The function calculates the Data Flow Models. To this end, Switch vertices are replaced by two models. One model has an edge from data port 1 to the output of the Switch block, 
  * the other an edge from data port 2 to the output of the Switch block. The other data flow port is equipped with a DataFlowOutputVertex instead of the Switch block, the control flow port is connected with 
  * a ControlFlowInputVertex. Both types are Output Vertices.
  */
  private def calculateDataFlowModels(l: List[SimulinkGraph]): List[SimulinkGraph] = l match {
    
    case (g::gs) => g.vertices.find { x => x.isInstanceOf[SwitchVertex] } match {
      case Some(v) => calculateDataFlowModels(g.replaceSwitchVertex(v.asInstanceOf[SwitchVertex],1)::g.replaceSwitchVertex(v.asInstanceOf[SwitchVertex],3)::gs)
      case _ => g::calculateDataFlowModels(gs)
    }
    case Nil => Nil
  }
  
}

