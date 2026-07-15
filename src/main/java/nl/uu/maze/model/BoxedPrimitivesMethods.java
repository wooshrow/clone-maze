package nl.uu.maze.model;

import java.lang.reflect.Type;
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
import sootup.core.types.PrimitiveType.FloatType;
import sootup.core.types.PrimitiveType.DoubleType;
import sootup.core.types.PrimitiveType.BooleanType; 

import sootup.core.types.PrimitiveType ;

/**
 * Provide symbolic models of common methods for boxed-primitives like Integer and Float.
 * Provided models: e.g. of methods x.intValue(), x.floatValue(), and valueOf(i).
 */
public class BoxedPrimitivesMethods {
	
	public static ModelOfMethod MODELof_Int_intValue 
	              = new IntegerLike_getValue(Integer.class, IntType.getInstance(),"intValue");
	
	public static ModelOfMethod MODELof_Int_valueOf  
				  = new IntegerLike_valueOf(Integer.class, IntType.getInstance(), Integer.TYPE) ;
	
	public static ModelOfMethod MODELof_Long_longValue
    			 = new IntegerLike_getValue(Long.class, LongType.getInstance(),"longValue");

	public static ModelOfMethod MODELof_Long_valueOf  
			     = new IntegerLike_valueOf(Long.class, LongType.getInstance(), Long.TYPE) ;

	
	public static ModelOfMethod MODELof_Float_floatValue 
    		     = new IntegerLike_getValue(Float.class, FloatType.getInstance(),"floatValue");

	public static ModelOfMethod MODELof_Float_valueOf  
    		     = new IntegerLike_valueOf(Float.class, FloatType.getInstance(), Float.TYPE) ;
	
	public static ModelOfMethod MODELof_Double_doubleValue 
    		    = new IntegerLike_getValue(Double.class, DoubleType.getInstance(),"doubleValue");

	public static ModelOfMethod MODELof_Double_valueOf  
    			= new IntegerLike_valueOf(Double.class, DoubleType.getInstance(), Double.TYPE) ;	
	
	public static ModelOfMethod MODELof_Boolean_booleanValue 
    		    = new IntegerLike_getValue(Boolean.class, BooleanType.getInstance(),"booleanValue");

	public static ModelOfMethod MODELof_Boolean_valueOf  
			    = new IntegerLike_valueOf(Boolean.class, BooleanType.getInstance(), Boolean.TYPE) ;	

	
	static private final JimpleToZ3Transformer jimpleToZ3 = new JimpleToZ3Transformer();
	
	
	public static class IntegerLike_getValue extends ModelOfMethod {
		String methodname ;
		String classname ;
		PrimitiveType sootTy ;
		
		@SuppressWarnings("rawtypes")
		IntegerLike_getValue(Class clazz, PrimitiveType sootTy, String getterName) {
			methodname = getterName ; 
			this.sootTy = sootTy ;
			classname = clazz.getName() ;
			try {
				this.method = clazz.getMethod(methodname) ;
			}
			catch(Exception e) {
				// swallow
			}
		}

		@Override
		public boolean match(MethodSignature sootSignature) {
			return sootSignature.getDeclClassType().getFullyQualifiedName().equals(classname)
				   && sootSignature.getName().equals(methodname) ;
		}
		
		@Override
		public SymbolicState executeModel(SymbolicState state, Local base, AbstractInvokeExpr expr) {
			MethodSignature methodSig = expr.getMethodSignature() ;
			if (! match(methodSig)) return null ;
			Expr<?> value = state.heap.getField(base.getName(), "value", sootTy) ;
	        // set the retval:
	        state.setReturnValue(value);
	        return state ;
		}
		
	}
	
	
	public static class IntegerLike_valueOf extends ModelOfMethod {

		String methodname ;
		String classname ;
		PrimitiveType sootTy ;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		IntegerLike_valueOf(Class clazz, PrimitiveType sootTy, Class primTy) {
			methodname = "valueOf" ;
			this.sootTy = sootTy ;
			classname = clazz.getName() ;
			try {
				this.method = clazz.getMethod(methodname, primTy) ; 
			}
			catch(Exception e) {
				// swallow
			}
		}
		
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

}
