package nl.uu.maze.model;

import java.lang.reflect.Method;
import java.util.Optional;

import nl.uu.maze.execution.symbolic.SymbolicState;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.signatures.MethodSignature;

/**
 * A common interface for models of methods. When symbolically executing a call
 * to a method m(x), if a model k(x) of m exists, we can choose to execute the
 * model instead. The model prescribes how to update the current symbolic state
 * in a single step, hence by-passing executing m. 
 * This is useful when m is complex, or when its code is outside MAZE's analysis
 * scope (e.g. if it is a library method).
 */
public abstract class AbsModelOfMethod {
	
	/**
	 * The method that is being modeled by this Model. 
	 */
	public Method method ;
	
	/**
	 * Return true is this Model is a model of the java-method of the given 
	 * Soot-signature.
	 */
	public abstract boolean match(MethodSignature sootSignature) ;
	
	/**
	 * Apply a direct update to the given symbolic state, modeling the behavior of
	 * {@link #method}. This should set the value returned by the method in the state's
	 * reserved field for retval, even if the retval is void. Or if the modeled behavior
	 * throws an exception, then set this in the state's exception status.
	 * <p>If the model cannot deal with the call-expression, this is indicated by 
	 * returning Empty.
	 * 
	 * @param state the symbolic state where {@link #method} is invoked.
	 * @param expr  the call/invocation expression 
	 * @param base  representation of the on which {@link #method} is called; so .. the o in o.m().
	 *              If m is a static method, this base o would be null. Also, at the Jimple level
	 *              /bytecode, o can only be a local variable (it can't be a complex expression).
	 * @return      
	 */
	public abstract Optional<SymbolicState> executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) ;

}
