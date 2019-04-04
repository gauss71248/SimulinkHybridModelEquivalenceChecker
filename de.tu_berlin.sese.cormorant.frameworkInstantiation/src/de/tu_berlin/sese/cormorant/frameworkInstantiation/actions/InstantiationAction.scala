package de.tu_berlin.sese.cormorant.frameworkInstantiation.actions

import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.IWorkbenchWindowActionDelegate
import org.eclipse.core.resources._
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime._
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.wizard.WizardDialog
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.IWorkbenchWindowActionDelegate
//import AbstractRepresentation._
import java.io.FileDescriptor
import java.io.PrintWriter
import java.io.PrintStream
import java.io.FileOutputStream

/**
 * @author gauss
 */
class InstantiationAction extends IWorkbenchWindowActionDelegate {

  var currentProject1, currentProject2: IProject = null

  def run(action: IAction): Unit = {
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)))
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)))
    

    import de.tu_berlin.pes.memo.MeMoPlugin
    import de.tu_berlin.pes.memo.project._
    if (currentProject1 == null || currentProject2 == null) {
      MeMoPlugin.out.println("Please select two projects");
      return ;
    }

    if (currentProject1.getPersistentProperty(ProjectNature.ACTIVEDATABASE) == null) {
      MeMoPlugin.out.println("Please select a Database");
      return ;
    }
    import de.tu_berlin.pes.memo.parser.persistence._
    import de.tu_berlin.pes.memo.model.impl._
    val persistence: MeMoPersistenceManager = MeMoPersistenceManager.getInstance();

    if (!currentProject1.getPersistentProperty(ProjectNature.ACTIVEDATABASE).equals(persistence.getDatabaseName()))
      persistence.switchDatabase(currentProject1.getPersistentProperty(ProjectNature.ACTIVEDATABASE));
   
    val m1: Model = persistence.getModel
    if (currentProject2.getPersistentProperty(ProjectNature.ACTIVEDATABASE) == null) {
      MeMoPlugin.out.println("Please select a Database");
      return ;
    }
   if (!currentProject2.getPersistentProperty(ProjectNature.ACTIVEDATABASE).equals(persistence.getDatabaseName()))
      persistence.switchDatabase(currentProject2.getPersistentProperty(ProjectNature.ACTIVEDATABASE));

    

    //try {
    
    val m2: Model = persistence.getModel
    import de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier._
    
 
    def executeFun(f: Any => Any, arg: Any,  n: Int, runtime: Long): Long = n match {
      case 0 => runtime
      case n => {
        val curtime: Long = System.currentTimeMillis()
        val result = f(arg)
        val delta = System.currentTimeMillis()-curtime
        executeFun(f,arg,n-1,runtime + delta)
      }
    }
    def averageExec(f: Any => Any, arg: Any, n: Int): Long = executeFun(f,arg,n,0)/n

    def test = {
      MeMoPlugin.out.println("[INFO] Time to execute Simulink Graph generation and flattening, average over 10 runs: " + averageExec(x => SimulinkGraph(x.asInstanceOf[Model]), m1, 10) + "ms")
      val sm: SimulinkGraph = SimulinkGraph(m1)
      MeMoPlugin.out.println("[INFO] Time to execute Data Flow Models, average over 10 runs: " + averageExec(x => new DataFlowModelCalculator(x.asInstanceOf[SimulinkGraph]).dataFlowModels, sm, 10) + "ms")
      val dm: Set[SimulinkGraph] = new DataFlowModelCalculator(sm).dataFlowModels
      MeMoPlugin.out.println(dm)
      import Partitioning._
      MeMoPlugin.out.println("[INFO] Time to execute Full Partitioning, average over 10 runs: " + averageExec(x => x.asInstanceOf[Set[SimulinkGraph]].map(y => connectDAGParts(equivalenceClassesPartitioning(prePartitioning(y, Nil.toSet)))), dm, 10) + "ms")
      val partitions = dm.map(x => connectDAGParts(equivalenceClassesPartitioning(prePartitioning(x, Nil.toSet))))
      MeMoPlugin.out.println("[INFO] Time to add interfaces, average over 10 runs: " + averageExec(x => x.asInstanceOf[Set[Partitioning]].map(y => EnrichedPartitioning.addAllInterfaces(y)), partitions, 10) + "ms")
      val interfacesAdded = partitions.map(p => EnrichedPartitioning.addAllInterfaces(p))
    }

    def test2 = {
      var total: Long = 0
      var curtime: Long = 0
      val sm1: SimulinkGraph = SimulinkGraph(m1)
      var delta: Long = 0
      //    val badoutblock = sm1.vertices.filter(p=>p.id==60).head
      //    val predbad = sm1.edges.filter(e=>e.target==badoutblock)
      //    MeMoPlugin.out.println("Bad Block: " + badoutblock)
      //    MeMoPlugin.out.println("Der Block ist: " + badoutblock.b)
      //    MeMoPlugin.out.println("Edge zum Block ausgehend von: " + predbad)
      //    
      //    
      val sm2: SimulinkGraph = SimulinkGraph(m2)

      curtime = System.currentTimeMillis()

      val dm1: Set[SimulinkGraph] = new DataFlowModelCalculator(sm1).dataFlowModels

      //     
      //     val dmSingle1: SimulinkGraph = dm1.head
      //    

      val dm2: Set[SimulinkGraph] = new DataFlowModelCalculator(sm2).dataFlowModels

      delta = System.currentTimeMillis() - curtime
      total = total + delta
      MeMoPlugin.out.println("Data Flow Model Calculation Time: " + delta)
      import Partitioning._

      curtime = System.currentTimeMillis()
      val par1 = dm1.map(g => EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(g, Nil.toSet))))).toList
      val par2 = dm2.map(g => EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(g, Nil.toSet))))).toList

      delta = System.currentTimeMillis() - curtime
      total = total + delta
      MeMoPlugin.out.println("Partitioning Calculation Time: " + delta)
      var totaltime: Long = 0
      for (i <- 1 to dm1.size) {
        var dm1akt = par1.head
        var dm2tail = par1.tail
        for (j <- 1 to dm2.size) {
          var dm2akt = par2.head
          var dm2tail = par2.tail
          curtime = System.currentTimeMillis()

          MeMoPlugin.out.println("Verification of Pair (" + i + "'" + j + "): " + VerificationEngine.verifySymbolicEquivalence(dm1akt, dm2akt))
          delta = System.currentTimeMillis() - curtime
          MeMoPlugin.out.println("Verification Time: " + delta)
          totaltime = totaltime + delta
        }
      }
      total = total + totaltime
      MeMoPlugin.out.println("Total Equivalence Verification Time: " + totaltime)
      MeMoPlugin.out.println("Total Time: " + total)
    }
    def test3 = {
      val sm1 = SimulinkGraph(m1)
      val sm2 = SimulinkGraph(m2)
      val dm1 = new DataFlowModelCalculator(sm1).dataFlowModels.head
      val dm2 = new DataFlowModelCalculator(sm2).dataFlowModels.head
      MeMoPlugin.out.println(dm1)
      import Partitioning._
      val vertex = dm1.vertices.filter(v => v.isInstanceOf[IntegratorVertex]).filter(v => v.asInstanceOf[IntegratorVertex].initialValue == 0).toSet
      val vertex2 = dm2.vertices.filter(v => v.isInstanceOf[ArithmeticVertex]).filter(v => v.asInstanceOf[ArithmeticVertex].fun == FunctionSymbol.SIN).toSet
      val par1 = EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dm1, vertex))))
      val par2 = EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dm2, vertex2))))
      MeMoPlugin.out.println(VerificationEngine.verifySymbolicEquivalence2(par1, par2))
    }
    //test3
    def test4 = {
    val sm1 = SimulinkGraph(m1)
      val sm2 = SimulinkGraph(m2)
      val dm1 = new DataFlowModelCalculator(sm1).dataFlowModels.head
      val dm2 = new DataFlowModelCalculator(sm2).dataFlowModels.head
       val vertex = dm1.vertices.filter(v => v.isInstanceOf[IntegratorVertex]).filter(v => v.asInstanceOf[IntegratorVertex].initialValue == 0).toSet
     val vertex2 = dm2.vertices.filter(v => v.isInstanceOf[ArithmeticVertex]).filter(v => v.asInstanceOf[ArithmeticVertex].fun == FunctionSymbol.SIN).toSet
       import Partitioning._
      val par1 = EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dm1, vertex))))
      val par2 = EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dm2, vertex2))))
      def relevantTerm(t: Term): Boolean = t match {
        case x: StateVariable => true
        case x: ArithmeticFunction => x.arguments.foldLeft(false)((b,y)=> b || relevantTerm(y))
        case _ => false
      }
    }
    
    
    def testCaseStudyF14 = {
      val sm1 = SimulinkGraph(m1)
      MeMoPlugin.out.println("Time for source model setup: " + executeFun(x=> SimulinkGraph(x.asInstanceOf[Model]), m1,1,0))
      val sm2 = SimulinkGraph(m2)
      MeMoPlugin.out.println("Time for target model setup: " + executeFun(x=> SimulinkGraph(x.asInstanceOf[Model]), m2,1,0))
      val dm1 = new DataFlowModelCalculator(sm1).dataFlowModels
      MeMoPlugin.out.println("Time for data flow model 1 setup: " + executeFun(x=> new DataFlowModelCalculator(x.asInstanceOf[SimulinkGraph]).dataFlowModels, sm1,1,0))
      val dm2 = new DataFlowModelCalculator(sm2).dataFlowModels
      MeMoPlugin.out.println("Time for data flow model 2 setup: " + executeFun(x=> new DataFlowModelCalculator(x.asInstanceOf[SimulinkGraph]).dataFlowModels, sm2,1,0))
      import Partitioning._
      val par1 = dm1.map(g => EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(g, Nil.toSet))))).toList
       MeMoPlugin.out.println("Time for partitioning 1 setup: " + executeFun(x=>x.asInstanceOf[Set[SimulinkGraph]].map(g => EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(g, Nil.toSet))))).toList, dm1,1,0))
       val par2 = dm2.map(g => EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(g, Nil.toSet))))).toList
       MeMoPlugin.out.println("Time for partitioning 2 setup: " + executeFun(x=>x.asInstanceOf[Set[SimulinkGraph]].map(g => EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(g, Nil.toSet))))).toList, dm2,1,0))
      var total: Long = 0
      var curtime: Long = 0
      var totaltime: Long = 0
       var delta: Long = 0
      for (i <- 1 to dm1.size) {
        var dm1akt = par1.head
        var dm2tail = par1.tail
        for (j <- 1 to dm2.size) {
          var dm2akt = par2.head
          var dm2tail = par2.tail
          curtime = System.currentTimeMillis()

          MeMoPlugin.out.println("Verification of Pair (" + i + "'" + j + "): " + VerificationEngine.verifySymbolicEquivalence(dm1akt, dm2akt))
          delta = System.currentTimeMillis() - curtime
          MeMoPlugin.out.println("Verification Time: " + delta)
          totaltime = totaltime + delta
        }
      }
      total = total + totaltime
      MeMoPlugin.out.println("Total Equivalence Verification Time: " + totaltime)
      MeMoPlugin.out.println("Total Time: " + total)
      MeMoPlugin.out.println("Global Epsilon: " + EpsilonCalculator.globalEpsilon(par1, par2, 10, 0.001))
    }
    // val sm1 = SimulinkGraph(m1)
     // val sm2 = SimulinkGraph(m2)
     // val dm1 = new DataFlowModelCalculator(sm1).dataFlowModels.head
     // val dm2 = new DataFlowModelCalculator(sm2).dataFlowModels.head
     // import Partitioning._
    //     val par1 = EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dm1, Set()))))
    //  val par2 = EnrichedPartitioning.addAllInterfaces(connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dm2, Set()))))
     testCaseStudyF14
     
      
   
   
//      MeMoPlugin.out.println(par1.partitions.filter(_.solved).map(p=>EpsilonCalculator.setUpMatrix(p).norm))
//      MeMoPlugin.out.println(par2.partitions.filter(_.solved).map(p=>EpsilonCalculator.obtainAnalyticalSolution(p)))
//      
//    MeMoPlugin.out.println(par2.partitions.filter(_.solved).map(p=>EpsilonCalculator.maxSecondDerivative(p, 10)))
//    val p1 = par1.partitions.filter(_.solved).head
//    val p2 = par2.partitions.filter(_.solved).head
//    MeMoPlugin.out.println(EpsilonCalculator.localEpsilon(p1, p2, 10, 0.001))
//    MeMoPlugin.out.println(EpsilonCalculator.setUpMatrix(p1).mathematicaString)
//    MeMoPlugin.out.println(EpsilonCalculator.setUpMatrix(p2).mathematicaString)
//    MeMoPlugin.out.println("_____")
//    MeMoPlugin.out.println(EpsilonCalculator.setUpInputVector(p1).mathematicaString)
//    MeMoPlugin.out.println(EpsilonCalculator.setUpInputVector(p2).mathematicaString)
//    MeMoPlugin.out.println(EpsilonCalculator.conditionalError(p1, 1, 10))
//    MeMoPlugin.out.println(EpsilonCalculator.propagatedEpsilon(p1, 1, 10, 0.01))
//    MeMoPlugin.out.println("__")
//    MeMoPlugin.out.println(EpsilonCalculator.globalEpsilon(par1, par2, 10, 0.01))
    //MeMoPlugin.out.println(par1.partitions.map(p=>"Partition " + p + " is solved?" + p.solved + "\n"))
    
  
     //MeMoPlugin.out.println(VerificationEngine.verifySymbolicEquivalence(par1, par2))
//    // MeMoPlugin.out.println(par1.size)
//     //MeMoPlugin.out.println(par2.size)
//     
//     val parsource1 = par1.head
//     val parsource2 = par1.tail.head
//     
//     val partarget1 = par2.head
//     val partarget2 = par2.tail.head
//     
//     
//     MeMoPlugin.out.println(parsource1.graphviz)
////     
//     val dmSingle2: SimulinkGraph = dm2.head
//     import Partitioning._
//   //  val partitionings = dm.map(x => connectDAGParts(equivalenceClassesPartitioning(prePartitioning(x, Nil.toSet))))
//     
//     val partitioning1 = connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dmSingle1, Nil.toSet)))
//     MeMoPlugin.out.println(partitioning1)
//     //val fullPartitionings = partitionings.map(p => EnrichedPartitioning.addAllInterfaces(p))
//     
//     val fullPartitioning1 = EnrichedPartitioning.addAllInterfaces(partitioning1)
//     
//      val partitioning2 = connectDAGParts(equivalenceClassesPartitioning(prePartitioning(dmSingle2, Nil.toSet)))
//     MeMoPlugin.out.println(partitioning2)
//     //val fullPartitionings = partitionings.map(p => EnrichedPartitioning.addAllInterfaces(p))
//     
//     val fullPartitioning2 = EnrichedPartitioning.addAllInterfaces(partitioning2)
//
//     
     def isStatefulPartition(p: Partition): Boolean = p.elements.find(x=>x.isInstanceOf[StatefulVertex]) match {
      case None => false
      case _ => true
    }
    def findStatefulPartition(p: Partitioning): Partition = p.partitions.find(isStatefulPartition) match {
      case Some(par) => par
      case None => new Partition(p.graph,p.graph.vertices,false)
    }
//    val testPartition1 = findStatefulPartition(fullPartitioning1)
//    val testPartition2 = findStatefulPartition(fullPartitioning2)
//  
   
     

  
//    
//    val p1 = testPartition1
//    val p2 = testPartition2
//    def evalMatrixVector(m: Matrix, v: Vector): String = "Inverse[IdentityMatrix[" + m.dim + "]-" + m.mathematicaString + "]" + "." + v.mathematicaString 
//    val matrix1 = ExpressionExtractor.setUpMatrix(p1)
//     val matrix2 = ExpressionExtractor.setUpMatrix(p2)
//     val inputVector1 = ExpressionExtractor.setUpInputVector(p1)
//     val inputVector2 = ExpressionExtractor.setUpInputVector(p2)
//     val obs1 = ExpressionExtractor.observations(p1).map(x=>x._2)
//     val obs2 = ExpressionExtractor.observations(p2).map(x=>x._2)
//     
//     MeMoPlugin.out.println(fullPartitioning1.graphviz)
//     
//     MeMoPlugin.out.println("________________________________")
//     
//     MeMoPlugin.out.println(fullPartitioning2.graphviz)
//   MeMoPlugin.out.println("Proof Obligations: " + VerificationEngine.symbolicProofObligations(p1, p2))
//   val verificationString = VerificationEngine.symbolicProofObligations(p1, p2).head.head
//  
//   MeMoPlugin.out.println("Ein run Mathematica: Kommando: " + verificationString + " ergibt " + MathematicaProcessor.runCommand(verificationString))
//   //MeMoPlugin.out.println("Verification Symbolic Equivalence Result: " + VerificationEngine.verifySymbolicEquivalence(p1, p2))
   //MeMoPlugin.out.println("Observations p1:" + ExpressionExtractor.observations(p1))
   //////////////////
//   MeMoPlugin.out.println(VerificationEngine.verifySymbolicEquivalence(p1, p2))
//   MeMoPlugin.out.println(VerificationEngine.symbolicEquivalenceVerificationPartialResults(p1,p2))
//   
//   MeMoPlugin.out.println(VerificationEngine.symbolicProofObligations(p1, p2).map(x=> x.map(y=> y + "\n") + "________\n"))
//   
   
   ///////////
   //  MeMoPlugin.out.println(fullPartitioning1.partitions.map(p=>p.elements.filter(a=>a.isInstanceOf[VariableInputVertex]).map(x=>ExpressionExtractor.obtainTerm(p, x)) + "\n"))
 //MeMoPlugin.out.println("Partitionsizes: " + fullPartitioning1.partitions.map(p=>p.elements.size)) 

     //__________
    // MeMoPlugin.out.println(VerificationEngine.verifySymbolicEquivalence(fullPartitioning1, fullPartitioning2)) 
 
     
     // MeMoPlugin.out.println("__________")
 // MeMoPlugin.out.println(VerificationEngine.symbolicEquivalenceVerificationPartialResults(fullPartitioning1, fullPartitioning2))
  
  }

 def selectionChanged(action: IAction, selection: ISelection): Unit = {
   import de.tu_berlin.pes.memo.MeMoPlugin 
   var s : IStructuredSelection = selection.asInstanceOf[IStructuredSelection]
    var first : IResource = s.toList().get(0).asInstanceOf[IResource]
    currentProject1 = first.getProject
    
    var second : IResource = s.toList().get(1).asInstanceOf[IResource]
    currentProject2 = second.getProject
    MeMoPlugin.out.println("First Selection:" + first)
    MeMoPlugin.out.println("Second Selection:" + second)
    
  }
//  def selectionChanged(action: IAction, selection: ISelection): Unit = {
//    selection match {
//      case s: IStructuredSelection =>  s.getFirstElement match {
//        case t: IResource => currentProject1=t.getProject
//        case _ => Unit
//      }
//      case _ => Unit
//    }
//    
//  }
  
  def dispose(): Unit = {}
  
  def init(window: IWorkbenchWindow): Unit = {}
}