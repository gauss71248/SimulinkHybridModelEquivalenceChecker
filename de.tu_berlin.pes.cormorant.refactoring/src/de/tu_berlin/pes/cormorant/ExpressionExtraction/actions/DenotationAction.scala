package de.tu_berlin.pes.cormorant.ExpressionExtraction.actions

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
import AbstractRepresentation._
import java.io.FileDescriptor
import java.io.PrintWriter
import java.io.PrintStream
import java.io.FileOutputStream

/**
 * @author gauss
 */
class DenotationAction extends IWorkbenchWindowActionDelegate {

  var currentProject: IProject = null

  def run(action: IAction): Unit = {
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)))
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)))
    /* import de.tu_berlin.pes.memo._
   // MeMoPlugin.out.println("Hello World from Scala-Dummy")
    val c1=new Variable(1,0)
    val c2=new Variable(2,2)
    val f = new Function(FunctionType.ADD,List(c1,c2))
    val g = new Function(FunctionType.MULT,List(f,new Constant(5)))
    val h = new Function(FunctionType.INVERT,List(g))
    val i = new Function(FunctionType.EXP,List(h))
    val l=new Variable(1,0)
    val eq = new Equation(l,i,true)
    import de.tu_berlin.pes.memo._
    MeMoPlugin.out.println(eq)
    import FunctionType._
    val a= Function(ADD, List(Variable(3,3),Function(MULT, List(Variable(2,2),Constant(3)))))
    MeMoPlugin.out.println(a)
    MeMoPlugin.out.println(a.arity)
    */

    import de.tu_berlin.pes.memo.MeMoPlugin
    import de.tu_berlin.pes.memo.project._
    if (currentProject == null) {
      MeMoPlugin.out.println("Please select project");
      return ;
    }

    if (currentProject.getPersistentProperty(ProjectNature.ACTIVEDATABASE) == null) {
      MeMoPlugin.out.println("Please select a Database");
      return ;
    }
    import de.tu_berlin.pes.memo.parser.persistence._

    val persistence: MeMoPersistenceManager = MeMoPersistenceManager.getInstance();

    if (!currentProject.getPersistentProperty(ProjectNature.ACTIVEDATABASE).equals(persistence.getDatabaseName()))
      persistence.switchDatabase(currentProject.getPersistentProperty(ProjectNature.ACTIVEDATABASE));

    import de.tu_berlin.pes.memo.model.impl._

    //try {
    val m: Model = persistence.getModel();
   
    val den = de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation.SimulinkGraph(m)
    //MeMoPlugin.out.println(den)
    
    val b: de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation.SimulinkBlock = den.vertices.find { case de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation.SimulinkBlock(a,_) => a.getType=="Sum" } match {
      case Some(a) => a
      case _ => de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation.SimulinkBlock(new Block, 0)
    }
    
    val out: de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation.SimulinkBlock = den.vertices.find { x => x.generalType == de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation.GeneralType.Output }.head
    //MeMoPlugin.out.println(b)
    //MeMoPlugin.out.println(den.predecessor(b)(1))
    //MeMoPlugin.out.println(den.obtainTerm(b))
    MeMoPlugin.out.println(den.denotation(out))
    
  //  MeMoPlugin.out.println(ar.vertices.filter { x => x.block.getType=="Constant" } map {x => x.block.getParameter("Value").toDouble})
 //  MeMoPlugin.out.println(ar.vertices.filter { x => x.block.getType=="Constant" } map  {x => ar.getAR(x)})
  //  MeMoPlugin.out.println(ar.vertices.filter { x => x.block.getType=="Sum" } map  {x => ar.getAR(x)})
  //  MeMoPlugin.out.println(ar.vertices.filter { x => x.block.getType=="Product" } map  {x => ar.getAR(x)})
     
 //import ModelType._
// MeMoPlugin.out.println(ar.vertices.filter { x => x.blocktype==Some(Unsampled) } map  {x => ar.getAR(x)})
 //   MeMoPlugin.out.println(ar.vertices.filter { x => x.block.getType=="Reference"}  map {x=>ar.getAR(x)}) 
//MeMoPlugin.out.println(ar.vertices.filter { x => x.blocktype==Some(Discrete)}  map {x=>ar.getAR(x)}) 
   
// MeMoPlugin.out.println(ar.vertices filter {x=> x.block.getType=="Product"} map { b => ar.getAR(b) })
 
 //MeMoPlugin.out.println(den)
 //MeMoPlugin.out.println(ar.test2)
  // MeMoPlugin.out.println(ar.test)
  //   MeMoPlugin.out.println(ar.blocks)
   //  MeMoPlugin.out.println(ar.edges)
   /* val labelid_outs = ar.vertices filter {x=>x.block.getType=="Math"} map {x => ar.graph.find(x) match {
      case Some(b) => b.outgoing.head.label.toString.toInt
      case _ => -1
      }
    }
    
    val labelid_args = ar.vertices filter {x=>x.block.getType=="Math"} map {x => ar.graph.find(x) match {
      case Some(b) => b.incoming map { x => x.label.toString.toInt}
      case _ => Set()
      }
    }
    val terms = ar.vertices.find {x=>x.block.getType=="Math"} match {
      case Some(b) => ar.setUpArgumentTerms4AR(labelid_args, b.block.getParameter("Inputs"))
      case _ => List()
    }
*/
    //MeMoPlugin.out.println(graph.vertices)
    //MeMoPlugin.out.println(graph.edges)
    

  }


  def selectionChanged(action: IAction, selection: ISelection): Unit = {
    selection match {
      case s: IStructuredSelection =>  s.getFirstElement match {
        case t: IResource => currentProject=t.getProject
      }
    }
    
  }
  
  def dispose(): Unit = {}
  
  def init(window: IWorkbenchWindow): Unit = {}
}