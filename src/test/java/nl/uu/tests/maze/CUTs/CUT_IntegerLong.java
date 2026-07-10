package nl.uu.tests.maze.CUTs;

/**
 * Integer and Long are objects (rather than primitives). Creating their instances is an
 * extra challenge for MAZE as they do not have constructors, though they do have factory
 * method.
 * 
 * This CUT tests MAZE handling of Integer and Long.
 */
public class CUT_IntegerLong {

	public static Integer IntegerParam(Integer x) {
		if (x == null)
			return null ;
		if (x < 9)
			return x ;
		return 1 ;
	}
	
	public static Integer IntegerLocalCons(Integer x) {
		Integer y = 11 ;
		if (x+y < 9)
			return x+y ;
		return 1 ;
	}
	
	public static Long LongParam(Long x) {
		if (x == null)
			return null ;
		if (x < 9)
			return x ;
		// cannot gen this yet. TODO
		return 1L ;
	}
	
	public static Long LongLocalCons(Long x) {
		Long y = 11L ;
		if (x+y < 9)
			return x+y ;
		return 1L ;
	}
	
}
