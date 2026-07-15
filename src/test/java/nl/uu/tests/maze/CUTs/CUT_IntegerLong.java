package nl.uu.tests.maze.CUTs;

/**
 * Boxed-primitives are classes like Integer and Long. Their instances are objects 
 * that wrap around the underlying primitive values they represent. Creating their 
 * instances is an extra challenge for MAZE as they do not have constructors, 
 * though they do have factory method. Also, the primitive value they wrap around
 * cannot be accessed through a field
 * 
 * This CUT tests MAZE handling of Boxed primitives.
 */
public class CUT_IntegerLong {
	
	/*
	public static class MYinteger {
		public int val ;
		public MYinteger(int k) { val = k+3 ; }
	}
	*/
	
	
	public static Integer IntegerParam(Integer x) {
		if (x == null)
			return null ;
		if (x == 9)
			return x ;
		return 1 ;
	}

	public static Integer IntegerLocalCons(Integer x) {
		Integer y = 11 ;
		if (x+y == 10)
			return x+y ;
		return 2 ;
	}
	
	public static Long LongParam(Long x) {
		if (x == null)
			return null ;
		if (x == 19)
			return x ;
		return 3L ;
	}
	
	public static Long LongLocalCons(Long x) {
		Long y = 11L ;
		if (x+y == 20)
			return x+y ;
		return 4L ;
	}
	
	public static Boolean BooleanParam(Boolean x) {
		if (x == null)
			return null ;
		if (x == true)
			return x ;
		return false ;
	}

	public static Boolean BooleanLocalCons(Boolean x) {
		Boolean y = true ;
		if (x && y)
			return x ;
		return false ;
	}
		
}
