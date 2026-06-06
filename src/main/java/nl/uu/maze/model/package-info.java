/**
 * Provide a framework to symbolically model methods. When MAZE symbolically executes a call 
 * to a method m(x), if a model of m exists, let's call it k(x), then the model k(x) can be 
 * used instead. The model contains instructions how to directly update the current symbolic 
 * state to model m's side effect, and what symbolic expression m would return. So, the 
 * symbolic execution will not go into m's actual's body. Or, if m is e.g. a library method, 
 * where normally MAZE would then fall back to concretely executing it, it now handles m 
 * symbolically through the model k.
 * 
 * <p>The model k(x) is meant to be symbolic.
 */
package nl.uu.maze.model;