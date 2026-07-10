package nl.uu.maze.model;

import java.util.Optional;

import com.microsoft.z3.Expr;

import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.transform.JimpleToZ3Transformer;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType.IntType;
import sootup.core.types.PrimitiveType.LongType;


public class IntegerLikeMethods {
	
	public static ModelOfMethod MODELof_Int_intValue = new Integer_intValue();
	public static ModelOfMethod MODELof_Int_valueOf  = new Integer_valueOf();
	public static ModelOfMethod MODELof_Long_longValue = new Long_longValue();
	public static ModelOfMethod MODELof_Long_valueOf  = new Long_valueOf();
	
	static private final JimpleToZ3Transformer jimpleToZ3 = new JimpleToZ3Transformer();
	
	public static class Integer_intValue extends ModelOfMethod {
		
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

		boolean match(MethodSignature sootSignature) {
			return sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
				   && sootSignature.getName().equals(methodname) ;
		}
		
		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			Expr<?> value = state.heap.getField(base.getName(), "value", IntType.getInstance()) ;
	        // set the retval:
	        state.setReturnValue(value);
	        return state ;
		}
		
	}
	
	
	public static class Integer_valueOf extends ModelOfMethod {

		String methodname ;
		String classname ;
		
		Integer_valueOf() {
			methodname = "valueOf" ;
			classname = Integer.class.getName() ;
			try {
				this.method = Integer.class.getMethod(methodname, Integer.TYPE) ;
			}
			catch(Exception e) {
				// swallow
			}
		}
		
		boolean match(MethodSignature sootSignature) {
			if (sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
					   && sootSignature.getName().equals(methodname)) {
				var tys = sootSignature.getParameterTypes() ;
				if (tys.size() == 1) {
					return IntType.getInstance().equals(tys.get(0))	;
				}
			}
			return false ;
		}

		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			ClassType IntegerSootTy = methodSig.getDeclClassType() ;
			Immediate arg0 = expr.getArg(0) ;
        	Expr<?> arg0Expr = jimpleToZ3.transform(arg0, state);
        	// allocate a new Integer in the sym-heap:
        	Expr<?> refToNewObj = state.heap.allocateObject(IntegerSootTy) ;
        	state.heap.setField(refToNewObj, "value", arg0Expr, IntegerSootTy) ;
        	// set the ref to the new Integer as the retval:
        	state.setReturnValue(refToNewObj);
            return state ;
		}
		
	}
	
	
	public static class Long_longValue extends ModelOfMethod {
		
		String methodname ;
		String classname ;
		
		Long_longValue() {
			methodname = "longValue" ;
			classname = Long.class.getName() ;
			try {
				this.method = Long.class.getMethod(methodname) ;
			}
			catch(Exception e) {
				// swallow
			}
		}

		boolean match(MethodSignature sootSignature) {
			return sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
				   && sootSignature.getName().equals(methodname) ;
		}
		
		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			Expr<?> value = state.heap.getField(base.getName(), "value", LongType.getInstance()) ; 
	        // set the retval:
	        state.setReturnValue(value);
	        return state ;
		}
		
	}
	
	public static class Long_valueOf extends ModelOfMethod {

		String methodname ;
		String classname ;
		
		Long_valueOf() {
			methodname = "valueOf" ;
			classname = Long.class.getName() ;
			try {
				this.method = Long.class.getMethod(methodname, Long.TYPE) ;
			}
			catch(Exception e) {
				// swallow
			}
		}
		
		boolean match(MethodSignature sootSignature) {
			if (sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
					   && sootSignature.getName().equals(methodname)) {
				var tys = sootSignature.getParameterTypes() ;
				if (tys.size() == 1) {
					return LongType.getInstance().equals(tys.get(0))	;
				}
			}
			return false ;
		}

		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			ClassType LongSootTy = methodSig.getDeclClassType() ;
			Immediate arg0 = expr.getArg(0) ;
        	Expr<?> arg0Expr = jimpleToZ3.transform(arg0, state);
        	// allocate a new Long in the sym-heap:
        	Expr<?> refToNewObj = state.heap.allocateObject(LongSootTy) ;
        	state.heap.setField(refToNewObj, "value", arg0Expr, LongSootTy) ;
        	// set the ref to the new Long as the retval:
        	state.setReturnValue(refToNewObj);
            return state ;
		}
		
	}

}
