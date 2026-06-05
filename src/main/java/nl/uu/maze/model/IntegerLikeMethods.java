package nl.uu.maze.model;

import java.util.Optional;

import com.microsoft.z3.Expr;

import nl.uu.maze.execution.symbolic.SymbolicState;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.PrimitiveType.IntType;

public class IntegerLikeMethods {
	
	public static Integer_intValue MODELof_IntValue = new Integer_intValue();
	
	public static class Integer_intValue extends AbsModelOfMethod {
		
		String methodname ;
		String classname ;
		
		Integer_intValue() {
			methodname = "intValue" ;
			classname = Integer.class.getName() ;
			try {
				this.method = Integer.class.getMethod(methodname) ;
			}
			catch(Exception e) {
				// swallow
			}
		}

		@Override
		public boolean match(MethodSignature sootSignature) {
			return sootSignature.getDeclClassType().getClassName().equals(methodname)
				   && sootSignature.getName().equals(methodname) ;
		}

		@Override
		public Optional<SymbolicState> executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			 Expr<?> value = state.heap.getField(base.getName(), "value", IntType.getInstance()) ;
	         // set the retval, and return empty as if the call is concrete:
	         state.setReturnValue(value);
	         return Optional.of(state) ;
		}
		
	}

}
