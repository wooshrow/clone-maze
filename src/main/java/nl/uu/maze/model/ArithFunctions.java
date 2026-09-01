package nl.uu.maze.model;

import com.microsoft.z3.ArithSort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPSort;
import com.microsoft.z3.RealExpr;

import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.transform.JimpleToZ3Transformer;
import nl.uu.maze.util.Z3ContextProvider;
import nl.uu.maze.util.Z3Sorts;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.PrimitiveType.DoubleType;
import sootup.core.types.PrimitiveType.IntType;

public class ArithFunctions {
	
	static private final JimpleToZ3Transformer jimpleToZ3 = new JimpleToZ3Transformer();

	private static final Context ctx() { return Z3ContextProvider.getContext(); }

	
	public static ModelOfMethod MODELof_SqRoot = new SqRoot();
	public static ModelOfMethod MODELof_DoublePow = new DoublePow();

	// can be expensive to solve... 
	public static class SqRoot extends ModelOfMethod {

		String methodname = "sqrt";
		String classname = "java.lang.Math";
		PrimitiveType sootTy = DoubleType.getInstance() ;
		
		SqRoot() { }
		
		@Override
		public boolean match(MethodSignature sootSignature) {
			if (sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
					   && sootSignature.getName().equals(methodname)) {
				var tys = sootSignature.getParameterTypes() ;
				if (tys.size() == 1) {
					return sootTy.equals(tys.get(0))	;
				}
			}
			return false ;
		}

		@SuppressWarnings("unchecked")
		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			//ClassType MySootTy = methodSig.getDeclClassType() ;
			Immediate arg0 = expr.getArg(0) ;
        	Expr<FPSort> arg0Expr = (Expr<FPSort>) jimpleToZ3.transform(arg0, state);
        	state.setReturnValue(ctx().mkFPSqrt(ctx().mkFPRoundNearestTiesToEven(), arg0Expr));
            return state ;
		}
		
	}
	
	
	
	/** 
	 * Model of Math.pow. But ... well don't expect miracle. This is very challenging for
	 * z3 to solve, if it can solve it at all.
	 * 
	 * Well.. disabling this. This seems to may cause z3 to hang (or take a looong time
	 */
	public static class DoublePow extends ModelOfMethod {

		String methodname = "pow";
		String classname = "java.lang.Math";
		PrimitiveType sootTy = DoubleType.getInstance() ;
		
		DoublePow() { }
		
		@Override
		public boolean match(MethodSignature sootSignature) {
			if (sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
					   && sootSignature.getName().equals(methodname)) {
				var tys = sootSignature.getParameterTypes() ;
				if (tys.size() == 2) {
					return sootTy.equals(tys.get(0)) && sootTy.equals(tys.get(1))	;
				}
			}
			return false ;
		}

		@SuppressWarnings("unchecked")
		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			Immediate arg0 = expr.getArg(0) ;
			Immediate arg1 = expr.getArg(1) ;
        	Expr<FPSort > arg0Expr = (Expr<FPSort >) jimpleToZ3.transform(arg0, state);
        	Expr<FPSort > arg1Expr = (Expr<FPSort >) jimpleToZ3.transform(arg1, state);
        	RealExpr result =  (RealExpr) ctx().mkPower(ctx().mkFPToReal(arg0Expr), ctx().mkFPToReal(arg0Expr)) ;
        	state.setReturnValue(ctx().mkFPToFP(
        			ctx().mkFPRoundNearestTiesToEven(), 
        			result, 
        			Z3Sorts.getInstance().getDoubleSort()));
            return state ;
		}
		
	}
	
}
