package nl.uu.tests.maze.CUTs;

/**
 * Float and Double are objects (rather than primitives). Creating their instances is an
 * extra challenge for MAZE as they do not have constructors, though they do have factory
 * method.
 * 
 * This CUT tests MAZE handling of Float and Double.
 */
public class CUT_FloatDouble {

	public static Float FloatParam(Float x) {
		if (x == null)
			return null ;
		if (x == 9.9)
			return x ;
		return 1.1f ;
	}
	
	public static Float FloatLocalCons(Float x) {
		Float y = 11f ;
		if (x+y == 10.9)
			return x+y ;
		return 2.1f ;
	}
	
	public static Double DoubleParam(Double x) {
		if (x == null)
			return null ;
		if (x == 19.7)
			return x ;
		return 3.7d ;
	}
	
	public static Double LongLocalCons(Double x) {
		Double y = 11d ;
		if (x+y == 20.7)
			return x+y ;
		return 4.7d ;
	}
	
}
