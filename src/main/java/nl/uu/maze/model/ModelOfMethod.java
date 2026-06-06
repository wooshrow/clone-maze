package nl.uu.maze.model;

import java.lang.reflect.Method;
import java.util.Optional;

import nl.uu.maze.execution.symbolic.SymbolicState;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;

/**
 * A common interface for symbolic models of methods. When symbolically executing a call
 * to a method m(x), if a model k(x) of m exists, we can choose to execute the
 * model instead. The model prescribes how to update the current symbolic state
 * in a single step, hence by-passing executing m. 
 * This is useful when m is complex, or when its code is outside MAZE's analysis
 * scope (e.g. if it is a library method).
 */
public abstract class ModelOfMethod {
	
	/**
	 * The method that is being modeled by this Model. 
	 */
	public Method method ;
	
	/**
	 * This method executeModel models the behavior of {@link #method} by applying
	 * a direct update to the given current symbolic state. This update may change
	 * the state of some objects in the head. It should also set the value returned 
	 * by the method in the reserved field (of the symbolic state) for retval, 
	 * even if the retval is void.
	 * executeModel then returns the new symbolic state. Or else it should it returns
	 * null if the execution-by-model fails.
	 * 
	 * <p> The given expr is the call expression, e.g. m(a,b,c). If m is NOT {@link #method},
	 * the model should handle the call, and this method executeModel should return null.
	 * Also, even if m is {@link #method}, but somehow the call cannot be handled by
	 * this model (e.g. because some of the parameters a,b,c do not fit the patterns that
	 * the model can handle), then executeModel should also indicate this by returning 
	 * null.
	 *  
	 * <p>If the modeled behavior throws an exception, then set this in the state's 
	 * exception status. Return the state (you should not return null, then).
	 * 
	 * @param state the symbolic state where {@link #method} is invoked.
	 * @param expr  the call/invocation expression 
	 * @param base  representation of the on which {@link #method} is called; so .. the o in o.m().
	 *              If m is a static method, this base o would be null. Also, at the Jimple level
	 *              /bytecode, o can only be a local variable (it can't be a complex expression).     
	 */
	public abstract SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) ;

}
