KeYmaera X
==========

Self-driving cars, autonomous robots, modern airplanes, or robotic surgery: we increasingly entrust our lives to computers and therefore should strive for nothing but the highest safety standards - mathematical correctness proof. Proofs for such cyber-physical systems can be constructed with the KeYmaera X prover. As a hybrid systems theorem prover, KeYmaera X analyzes the control program and the physical behavior of the controlled system together in differential dynamic logic.

KeYmaera X features a minimal core of just about 1700 lines of code that isolates all soundness-critical reasoning. Such a small and simple prover core makes it much easier to trust verification results. Pre-defined and custom tactics built on top of the core drive automated proof search. KeYmaera X comes with a web-based front-end that provides a clean interface for both interactive and automated proving, highlighting the most crucial parts of a verification activity. Besides hybrid systems, KeYmaera X also supports the verification of hybrid games in differential game logic.

More information and precompiled binaries are available at:
  http://keymaeraX.org/

* [Differential dynamic logic grammar](http://keymaerax.org/doc/dL-grammar.md)
* [Informal KeYmaera X Syntax](https://github.com/LS-Lab/KeYmaeraX-release/wiki/KeYmaera-X-Syntax-and-Informal-Semantics)

Installation
============
The easiest way to run KeYmaera X is to download binaries 
[keymaerax.jar](http://keymaerax.org/keymaerax.jar) and start from command line

    java -jar keymaerax.jar

First ensure that the following software is installed
- [Java Development Kit JDK](https://java.com/download)
  (version 1.8 recommended, versions 1.9-1.10 work as well but are not recommended. Mathematica 9.0 is only compatible with Java 1.6 and 1.7. Mathematica 10.0+ is also compatible with Java 1.8)
- [Wolfram Mathematica](https://www.wolfram.com/mathematica/)
  (version 10+ recommended. Previous versions may work.
  The Mathematica J/Link library that comes with Mathematica is needed during compilation. Mathematica needs to be activated to use it also at runtime.
  Without active Mathemetica, the [Z3 Solver](https://github.com/Z3Prover/z3) is automatically used for real arithmetic.)

#### Configuration
KeYmaera X requires a decision procedure for real arithmetic to finalize proofs. It is tested best with Mathematica and some features are only available when using Mathematica.
After starting KeYmaera X you can configure arithmetic tools in the _Help->Tool Configuration_ menu.

Depending on the operating system, Mathematica is installed in different locations. 
Alternatively, you can also specify which arithmetic tools to use from command line with
parameters `-mathkernel` and `-jlink`. Parameters that are appropriate when
Mathematica is installed in the default location are provided below.

#### Default Configuration Parameters per Operating System
Mac OS, 64bit, Mathematica 10.4+
* `-mathkernel /Applications/Mathematica.app/Contents/MacOS/MathKernel`
* `-jlink /Applications/Mathematica.app/Contents/SystemFiles/Links/JLink/SystemFiles/Libraries/MacOSX-x86-64`

Linux, 64bit, Mathematica 10.4+
* `-mathkernel /usr/local/Wolfram/Mathematica/10.4/Executables/MathKernel`
* `-jlink /usr/local/Wolfram/Mathematica/10.4/SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64`

Windows, 64bit, Mathematica 10.4+
* `-mathkernel "C:\Program Files\Wolfram Research\Mathematica\10.4\MathKernel.exe"`
* `-jlink "C:\Program Files\Wolfram Research\Mathematica\10.4\SystemFiles\Links\JLink\SystemFiles\Libraries\Windows-x86-64"`

#### FAQ: Run Problems

If running `java -jar keymaerax.jar` results in the error `java.lang.module.FindException: Module java.xml.bind not found` then downgrade JDK to version 1.8 till 1.10, because JDK 1.11 is not yet supported.

If running `java -jar keymaerax.jar` results in the error `Invalid or corrupt jarfile` then update to Java 1.8 and to Mathematica 10+.
If you need to use an earlier version of Java or Mathematica, you can also run KeYmaera X via

    java -Xss20M -cp keymaerax.jar KeYmaeraX

If KeYmaera X acts weird after an update, clean your local cache of lemmas by removing (or renaming) the directory `~/.keymaerax/cache`.
You could also try renaming the model and proof database `~/.keymaerax/keymaerax.sqlite` (if this file has become corrupt, it may prevent KeYmaera X from working properly). It is good practice to periodically export KeYmaera X proof archives, because they avoid database upgrade problems.

Errors related to `com.wolfram.jlink` or `JLinkNativeLibrary` are caused by incompatibilities of Java 1.8 in combination with Mathematica 9. It is recommended to use Mathematica 10+. Or they may be caused by operating system configuration issues.


Building
========
To compile KeYmaera X from source code and learn about faster incremental compilation in IDEs, see [Building Instructions](https://github.com/LS-Lab/KeYmaeraX-release/wiki/Building-Instructions).
In a nutshell, install the right software and run the following to build `keymaerax.jar`:

    sbt clean assembly

Publications
============

KeYmaera X implements the uniform substitution calculus for differential dynamic logic in order to enable soundness assurance by way of a small trusted LCF-style kernel while still being amenable to automatic theorem proving.

http://www.ls.cs.cmu.edu/publications.html

1. André Platzer. 
[A complete uniform substitution calculus for differential dynamic logic](https://doi.org/10.1007/s10817-016-9385-1).
Journal of Automated Reasoning, 59(2), pp. 219-266, 2017.

2. André Platzer.
[Logics of dynamical systems](https://doi.org/10.1109/LICS.2012.13).
ACM/IEEE Symposium on Logic in Computer Science, LICS 2012, June 25–28, 2012, Dubrovnik, Croatia, pages 13-24. IEEE 2012.

3. Nathan Fulton, Stefan Mitsch, Jan-David Quesel, Marcus Völp and André Platzer. 
[KeYmaera X: An axiomatic tactical theorem prover for hybrid systems](https://doi.org/10.1007/978-3-319-21401-6_36).
In Amy P. Felty and Aart Middeldorp, editors, International Conference on Automated Deduction, CADE'15, Berlin, Germany, Proceedings, LNCS. Springer, 2015. 

4. Nathan Fulton, Stefan Mitsch, Brandon Bohrer and André Platzer. 
[Bellerophon: Tactical theorem proving for hybrid systems](https://doi.org/10.1007/978-3-319-66107-0_14).
In Mauricio Ayala-Rincón and César Muñoz, editors, Interactive Theorem Proving, International Conference, ITP 2017, volume 10499 of LNCS, pp. 207-224. Springer, 2017. 

5. André Platzer.
[Logical Foundations of Cyber-Physical Systems](http://lfcps.org/lfcps/).
Springer, Cham, 2018.

The soundness assurances provided by a small LCF-style kernel are further strengthened by a cross-verification of the soundness theorem for the uniform substitution calculus in [Isabelle/HOL](https://github.com/LS-Lab/Isabelle-dL) and [Coq](https://github.com/LS-Lab/Coq-dL).

6. Brandon Bohrer, Vincent Rahli, Ivana Vukotic, Marcus Völp and André Platzer.
[Formally verified differential dynamic logic](https://doi.org/10.1145/3018610.3018616).
ACM SIGPLAN Conference on Certified Programs and Proofs, CPP 2017, Jan 16-17, 2017, Paris, France, pages 208-221, ACM, 2017.

A secondary goal of KeYmaera X is to also make it possible to implement extensions of differential dynamic logic, such as differential game logic for hybrid games as well as quantified differential dynamic logic for distributed hybrid systems:

7. André Platzer. 
[Differential game logic](https://doi.org/10.1145/2817824).
ACM Trans. Comput. Log., 17(1), 2015.

8. André Platzer. 
[Differential hybrid games](https://doi.org/10.1145/3091123).
ACM Trans. Comput. Log., 18(3), 2017.

9. André Platzer.
[A complete axiomatization of quantified differential dynamic logic for distributed hybrid systems](https://doi.org/10.2168/LMCS-8(4:17)2012).
Logical Methods in Computer Science, 8(4), pages 1-44, 2012.

This software uses faster generalized uniform substitution algorithms:

10. André Platzer.
[Uniform substitution for differential game logic](https://doi.org/10.1007/978-3-319-94205-6_15).
In Didier Galmiche, Stephan Schulz and Roberto Sebastiani, editors, Automated Reasoning, 9th International Joint Conference, IJCAR 2018, volume 10900 of LNCS, pp. 211-227. Springer 2018.

11. André Platzer.
[Uniform substitution at one fell swoop](https://lfcps.org/pub/dGL-usubst-one.pdf).
In Pascal Fontaine, editor, International Conference on Automated Deduction, CADE'19, volume 11716 of LNCS. Springer, 2019.

KeYmaera X uses the Pegasus tool for invariant generation [if the appropriate software is installed](http://pegasus.keymaeraX.org/):

12. Andrew Sogokon, Stefan Mitsch, Yong Kiam Tan, Katherine Cordwell and André Platzer. 
[Pegasus: A framework for sound continuous invariant generation](https://lfcps.org/pub/Pegasus.pdf). 
In Annabelle McIver and Maurice ter Beek, editors, FM 2019: Formal Methods - 23rd International Symposium, LNCS. Springer, 2019.

The design principles for the user interface of KeYmaera X are described in:

13. Stefan Mitsch and André Platzer. 
[The KeYmaera X proof IDE: Concepts on usability in hybrid systems theorem proving](https://doi.org/10.4204/EPTCS.240.5). 
In Catherine Dubois, Paolo Masci and Dominique Méry, editors, 3rd Workshop on Formal Integrated Development Environment F-IDE 2016, volume 240 of EPTCS, pp. 67-81, 2017.

Copyright and Licenses
======================

Copyright (C) 2014-2019 Carnegie Mellon University. See COPYRIGHT.txt for details.
Developed by Andre Platzer, Stefan Mitsch, Nathan Fulton, Brandon Bohrer, Jan-David Quesel, Yong Kiam Tan, Andrew Sogokon, Fabian Immler, Marcus Voelp, Ran Ji.

See LICENSE.txt for the conditions of using this software.

The KeYmaera X distribution contains external tools. A list of tools and their licenses can be found in

    keymaerax-webui/src/main/resources/license/tools_licenses

Contact
=======

KeYmaera X developers: keymaeraX@keymaeraX.org
