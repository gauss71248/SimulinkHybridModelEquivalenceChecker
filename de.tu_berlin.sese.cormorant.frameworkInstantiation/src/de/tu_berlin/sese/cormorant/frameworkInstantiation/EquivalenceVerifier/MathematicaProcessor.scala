

package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier


object MathematicaProcessor {
  import com.wolfram.jlink._
  //import de.tu_berlin.sese.cormorant.frameworkInstantiation.Utilities
  //import de.tu_berlin.pes.memo.MeMoPlugin
  def runSimpleCommand(s: String): String = {
    
    
					// TODO switch case OS
    //if (System.getProperty("os.name") == "Mac OS X") {
    // 
    //}
		val cmdLine: Array[String] = Array("-linkmode", "launch", "-linkname", "/Applications/Mathematica.app/Contents/MacOS/MathKernel -mathlink")
		val ml = MathLinkFactory.createKernelLink(cmdLine);
		ml.discardAnswer();
		
		ml.evaluateToOutputForm(s,0);
  }
   
   def runCommand(s: String): String = {
    
    
					// TODO switch case OS
    //if (System.getProperty("os.name") == "Mac OS X") {
    // 
    //}
		val cmdLine: Array[String] = Array("-linkmode", "launch", "-linkname", "/Applications/Mathematica.app/Contents/MacOS/MathKernel -mathlink")
		val ml = MathLinkFactory.createKernelLink(cmdLine);
		
		ml.discardAnswer();
		ml.evaluate(s)
		ml.waitForAnswer()
		val result = ml.getExpr
		ml.close()
		//ml.evaluateToOutputForm(result, 0)
		result.toString
  }
   
  def runCommands(l: List[String]): List[String] = l.map(runCommand)
}
  