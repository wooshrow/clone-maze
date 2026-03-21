/**
 * Contain selected classes from SV-Comp dataset: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
 * 
 * The classes are modified to match the way e.g. Maze treats arguments
 * of method-under-test. E.g. Maze wants to see an argument as an argument
 * (a variable like x,y,z), whereas in SV-Comp it invokes a method like
 * Verifier.nondetDouble(), which then has to be removed for Maze.
 */
package nl.uu.maze.benchmarks.svcomp;