Java Voronoi Treemap Library - fork
=====================

This project was forked from https://github.com/ArlindNocaj/Voronoi-Treemap-Library.

The original project *JVoroTreemap* is a fast standalone java library which computes Voronoi treemaps.

The following article contains the most important references related to that implementation:

* Arlind Nocaj, Ulrik Brandes, "Computing Voronoi Treemaps: Faster, Simpler, and Resolution-independent", Computer
  Graphics Forum, vol. 31, no. 3, June 2012, pp. 855-864

License
------------------------

Copyright (c) 2015 Arlind Nocaj, University of Konstanz.

All rights reserved. This program and the accompanying materials are made available under the terms of the GNU Public License v3.0 which accompanies this distribution, and is available at http://www.gnu.org/licenses/gpl.html

For distributors of proprietary software, other licensing is possible on request (with University of
Konstanz): <arlind.nocaj@gmail.com>

Citation
-----------------

This work is based on the publication below, please cite on usage:

* Arlind Nocaj, Ulrik Brandes, "Computing Voronoi Treemaps: Faster, Simpler, and Resolution-independent", Computer
  Graphics Forum, vol. 31, no. 3, June 2012, pp. 855-864

Fork
------------------------

Voronoi-Treemap-Library-FX is a fork by Daniel Huson. There are several changes:

- All usages of java.awt and java.swing have been removed
- The library has been reduced to the *core* calculation of a Voronoi map for a set of children
- The calculation of the tree has been reimplemented, mainly following the original implementation
- This fork provides a JavaFX service for calculating the Voronoi tree map
- The key algorithm for computing the tree remains the one implemented by Arlind Nocaj, so all credit goes to him
- Maven is now used.
