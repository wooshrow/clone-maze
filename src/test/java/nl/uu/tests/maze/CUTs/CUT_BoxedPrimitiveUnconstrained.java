package nl.uu.tests.maze.CUTs;

/**
 * Boxed-primitives are classes like Integer and Long. Their instances are
 * objects that wrap around the underlying primitive values they represent.
 * Creating their instances is an extra challenge for MAZE as they do not have
 * constructors, though they do have factory method. Also, the primitive value
 * they wrap around cannot be accessed through a field
 * 
 * This CUT tests MAZE handling of Boxed primitives.
 */
public class CUT_BoxedPrimitiveUnconstrained {

	public static int fooInt(Integer x) {
		x = x+2 ;
		return x+10 ;
	}

	public static long fooLong(Long x) {
		x = x+2 ;
		return x+10 ;
	}
	
	public static float fooFloat(Float x) {
		x = x+ 2.1f ;
		return x+10 ;
	}
	
	
	public static double fooDouble(Double x) {
		x = x+2.5d ;
		return x+10 ;
	}
	
	public static boolean fooBoolean(Boolean x) {
		x = !x ;
		return !x ;
	}

}
