# SimulinkHybridModelEquivalenceChecker
This repository contains the source code of my PhD thesis. The parser code is written in Java, partially in Scala. 
The core, which forms the equivalence checker, is written in Scala.

All modules are Eclipse extensions. A requirement is the Eclipse modelling distribution.
Beware, the coding is prototypic in nature and not very well documented. This is quite common in research projects.

## What does the software?
The packages written in Java parse MATLAB Simulink models and make them programmatically accessible in a Java class hierarchy. My Scala-based implementation gets two Simulink models and tries to verify their behavioural equivalence. If refactorings are located in time-continuous parts, this equivalence only holds in a perfect mathematical sense. Hence, my approach also calculates the maximal deviation of the signals in the simulation interval in these cases.

## Core Engine for Simulink Model Equivalence Checking

The core is at the path de.tu_berlin.sese.cormorant.frameworkInstantiation/src/de/tu_berlin/sese/cormorant/frameworkInstantiation/EquivalenceVerifier

de.tu-berlin.de.pes.cormorant.reactoring is also a Scala-based package being part of my PhD implementation
