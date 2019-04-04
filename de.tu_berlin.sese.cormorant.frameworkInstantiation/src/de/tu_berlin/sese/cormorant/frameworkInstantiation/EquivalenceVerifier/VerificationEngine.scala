

package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier

object VerificationEngine {
  

  
//  def permute[T](l1:List[T],l2:List[T]):Option[List[List[(T,T)]]] = {
//    def mergeLists(xs: List[T], ys: List[T], res: List[(T,T)]): List[(T,T)] = xs match {
//      case Nil => res
//      case (l::ls) => ys match {
//        case Nil => res
//        case (m::ms) => mergeLists(ls,ms,res.+:((l,m)))
//      }
//    }
//    val perms = l2.permutations.toList
//    l1.size match {
//      case a if a!=l2.size => None
//      case _ => Some(perms.map(x=> mergeLists(l1,x, Nil)))
//    }
//    
//  }
  
   def permutations[T](lst: List[T]): List[List[T]] = lst match {
    case Nil => List(Nil)
    case x :: xs => permutations(xs) flatMap { perm =>
      (0 to xs.length) map { num =>
        (perm take num) ++ List(x) ++ (perm drop num)
      }
    }
   }
  
  def permutations(n: Int)  = {
    def assign(i: Int, l: List[Int], res: List[(Int,Int)]): List[(Int, Int)] = l match {
      case Nil => res
      case (x::xs) => assign(i+1,xs, res.+:(i,x))
    }
     permutations[Int](List.range(1,n+1)).map(l=>assign(1,l.toList,Nil))
   
  }
   
  
  
//  
//  def symbolicProofObligations(p1: Partition, p2: Partition): Option[List[List[String]]] = {
//    val obs1 = ExpressionExtractor.observations(p1).map(x=>x._2)
//    val matrix1 = ExpressionExtractor.setUpMatrix(p1)
//    val inputVector1 = ExpressionExtractor.setUpInputVector(p1)
//    val obs2 = ExpressionExtractor.observations(p2).map(x=>x._2)
//    val matrix2 = ExpressionExtractor.setUpMatrix(p2)
//    val inputVector2 = ExpressionExtractor.setUpInputVector(p2)
//    def evalMatrixVector(m: Matrix, v: Vector): String = "Inverse[IdentityMatrix[" + m.dim + "]-" + m.mathematicaString + "]" + "." + v.mathematicaString 
//    def termReplacementString(observationTerm: String, idStateVariable: Int, m: Matrix, v: Vector): String = observationTerm + "/.y" + idStateVariable + "->(" + evalMatrixVector(m,v) + ")[[" + idStateVariable + "]]"
//    def termReplacementStrings(startTerm: Term, l: List[Int], m: Matrix, v: Vector, s: String): String = l match {
//      case Nil => startTerm.toString + s
//      case (x::xs) => termReplacementStrings(startTerm,xs,m,v, termReplacementString(s,x,m,v))
//    }
//    val obs1Strings = obs1.map(x=> termReplacementStrings(x,List.range(1, matrix1.dim),matrix1, inputVector1, ""))
//    val obs2Strings = obs2.map(x=> termReplacementStrings(x,List.range(1, matrix2.dim),matrix2, inputVector2, ""))
//    
//   // def constructProofObligationSets(l1: List[String], l2: List[String]):Option[List[List[String]]] = {
//      
//    //}
//    permute(obs1Strings,obs2Strings) match {
//      case None => None
//      case Some(x) => Some(x.map(a=>a.map(b=>"(" + b._1 + ")==(" + b._2 + ")")))
//    }
//  }
  
  /*
   * The function provides for each observation of the first partition a list of proof obligations (as String) that relate
   * an observation of the first with an observation of the second partition. Hence, symbolic equivalence can be verified 
   * as being correctly by finding one representative for each observation of the first partition that evaluates to true.
   */
  def symbolicProofObligations(p1: Partition, p2: Partition): List[List[String]] = {
    val obs1 = ExpressionExtractor.observations(p1).map(x=>x._2)
    val matrix1 = ExpressionExtractor.setUpMatrix(p1)
    val inputVector1 = ExpressionExtractor.setUpInputVector(p1)
    val obs2 = ExpressionExtractor.observations(p2).map(x=>x._2)
    val matrix2 = ExpressionExtractor.setUpMatrix(p2)
    val inputVector2 = ExpressionExtractor.setUpInputVector(p2)
    
    def evalMatrixVector(m: Matrix, v: Vector): String = "Inverse[IdentityMatrix[" + m.dim + "]-" + m.mathematicaString + "]" + "." + v.mathematicaString 
     
    
    
    def termReplacementString(idStateVariable: Int, m: Matrix, v: Vector): String =  "/.y" + idStateVariable + "->(" + evalMatrixVector(m,v) + ")[[" + idStateVariable + "]]"
    def termReplacementStrings(startTerm: Term, l: Range, m: Matrix, v: Vector, s: String): String = l.foldLeft(startTerm.toString())((s,x)=>"(" + s  + termReplacementString(x,m,v)+ ")") 

    val obs1Strings= obs1.map(x=> termReplacementStrings(x,1 to matrix1.dim,matrix1, inputVector1, ""))
    
    val obs2Strings =obs2.map(x=> termReplacementStrings(x,1 to matrix2.dim,matrix2, inputVector2, ""))
    
   
   // def constructProofObligationSets(l1: List[String], l2: List[String]):Option[List[List[String]]] = {
      
    //}
    
//    permute(obs1Strings,obs2Strings) match {
//      case None => None
//      case Some(x) => Some(x.map(a=>a.map(b=>"Reduce[" + b._1 + ")==(" + b._2 + "]")))
//    }
    obs1Strings.map(x=>obs2Strings.map(y=>"Reduce[" + x + "==" + y + "]"))
  }
  
  
  def symbolicProofObligations2(p1: Partition, p2: Partition): List[List[String]] = {
    val obs1 = ExpressionExtractor.observations(p1).map(x=>x._2)
    val matrix1= ExpressionExtractor.setUpMatrix(p1)
     
    val inputVector1 = ExpressionExtractor.setUpInputVector(p1)
    val obs2 = ExpressionExtractor.observations(p2).map(x=>x._2)
    val matrix2 = ExpressionExtractor.setUpMatrix(p2)
    val inputVector2 = ExpressionExtractor.setUpInputVector(p2)
    
    def evalMatrixVector(m: Matrix, v: Vector): String = "Inverse[IdentityMatrix[" + m.dim + "]-" + m.mathematicaString + "]" + "." + v.mathematicaString 
     
    
    
    def termReplacementString(idStateVariable: Int, m: Matrix, v: Vector): String =  "/.y" + idStateVariable + "->(" + evalMatrixVector(m,v) + ")[[" + idStateVariable + "]]"
    def termReplacementStrings(startTerm: Term, l: Range, m: Matrix, v: Vector, s: String): String = l.foldLeft(startTerm.toString())((s,x)=>"(" + s  + termReplacementString(x,m,v)+ ")") 

    val obs1Strings: List[String]= p1.solved match {
      
      case true if p1.elements.find(v=>v.isInstanceOf[StatefulVertex])==None => List("LaplaceTransform[" + obs1.head.mathematicaString + ",t,s]")
      case _ =>obs1.map(x=> termReplacementStrings(x,1 to matrix1.dim,matrix1, inputVector1, ""))
    
    }
    
    val obs2Strings: List[String] =p2.solved match {
      
      case true if p2.elements.find(v=>v.isInstanceOf[StatefulVertex])==None => List("LaplaceTransform[" + obs2.head.mathematicaString + ",t,s]")
      case _ => obs2.map(x=> termReplacementStrings(x,1 to matrix2.dim,matrix2, inputVector2, ""))
    }
    
   
 
    obs1Strings.map(x=>obs2Strings.map(y=>"Reduce[" + x + "==" + y + "]"))
  }
  
  def forall[T](l:List[T], p:T=>Boolean): Boolean = l.foldLeft(true)((b,x)=>b && p(x))
  
  def exists[T](l: List[T], p: T=> Boolean): Boolean = l.foldLeft(false)((b,x)=> b||p(x))
  /*
   * Verifies symbolic equivalence for two partitions. 
   */
  def verifySymbolicEquivalence(p1: Partition, p2: Partition): Boolean = ExpressionExtractor.observations(p1).size match {
    case a if a==ExpressionExtractor.observations(p2).size => forall[List[String]](symbolicProofObligations(p1,p2).map(x=>MathematicaProcessor.runCommands(x)), l=>exists[String](l,x=>x=="True"))
    case _ => false
  }
   def verifySymbolicEquivalence2(p1: Partition, p2: Partition): Boolean = ExpressionExtractor.observations(p1).size match {
    case a if a==ExpressionExtractor.observations(p2).size => forall[List[String]](symbolicProofObligations2(p1,p2).map(x=>MathematicaProcessor.runCommands(x)), l=>exists[String](l,x=>x=="True"))
    case _ => false
  }
  
  def symbolicEquivalenceVerificationPartialResults(p1:Partition, p2: Partition) = symbolicProofObligations(p1,p2).map(x=>MathematicaProcessor.runCommands(x)).map(x=> x.map(y=> y match {
    case a if a=="True" => true
    case _ => false}))
  
    def symbolicEquivalenceVerificationPartialResults2(p1:Partition, p2: Partition) = symbolicProofObligations2(p1,p2).map(x=>MathematicaProcessor.runCommands(x)).map(x=> x.map(y=> y match {
    case a if a=="True" => true
    case _ => false}))
  
   def verifySymbolicEquivalence(p1: Partitioning, p2: Partitioning): Boolean = forall[List[Boolean]](p1.partitions.filter(p=>p.elements.size>1).map(x=>p2.partitions.filter(p=>p.elements.size>1).map(y=> verifySymbolicEquivalence(x,y)).toList).toList, l=>exists[Boolean](l,a=>a))
  def verifySymbolicEquivalence2(p1: Partitioning, p2: Partitioning): Boolean = forall[List[Boolean]](p1.partitions.filter(p=>p.elements.size>1).map(x=>p2.partitions.filter(p=>p.elements.size>1).map(y=> verifySymbolicEquivalence2(x,y)).toList).toList, l=>exists[Boolean](l,a=>a))
  
   
   def symbolicEquivalenceVerificationPartialResults(par1:Partitioning, par2: Partitioning) = par1.partitions.map(p1=> par2.partitions.map(p2 => (p1.elements.size,p2.elements.size,verifySymbolicEquivalence(p1,p2))))
    def symbolicEquivalenceVerificationPartialResults2(par1:Partitioning, par2: Partitioning) = par1.partitions.map(p1=> par2.partitions.map(p2 => (p1.elements.size,p2.elements.size,verifySymbolicEquivalence2(p1,p2))))
  
  
  def verifySymbolicEquivalence(l1: List[Partitioning], l2: List[Partitioning]): Boolean = forall[List[Boolean]](l1.map(p1 => l2.map(p2 => verifySymbolicEquivalence(p1,p2))), l=> exists[Boolean](l,a=>a))

   def verifySymbolicEquivalence2(l1: List[Partitioning], l2: List[Partitioning]): Boolean = forall[List[Boolean]](l1.map(p1 => l2.map(p2 => verifySymbolicEquivalence2(p1,p2))), l=> exists[Boolean](l,a=>a))

  
  
  //  def MathematicaResults(p1: Partition, p2: Partition) = symbolicProofObligations(p1,p2) match {
//    case None => "funzt net"
//    case Some(x) => x.map(a=> MathematicaProcessor.runCommands(a))
//  }
//  def verifySymbolicEquivalence(p1: Partition, p2: Partition): Boolean = symbolicProofObligations(p1,p2) match {
//    case None => false
//    case Some(x) => x.map(a=> MathematicaProcessor.runCommands(a)).map(a=>a.foldLeft(true)((b,y)=>b&&y=="True")).foldLeft(false)((b,y)=>b||y)
//  }
  
  
}