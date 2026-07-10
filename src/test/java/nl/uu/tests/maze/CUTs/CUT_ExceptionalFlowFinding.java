package nl.uu.tests.maze.CUTs;

/**
 * CUT to test whether MAZE can find exceptional execution flow (passing through
 * throwing exception).
 */
public class CUT_ExceptionalFlowFinding {
	
	public int arrayIndexOutOfBound(int k) {
		int[] a = {0,1,2} ;
		if (k<a.length) {
			return a[k] ;
		}
		else
			return -1 ;
	}
	
	public int divByZero(int x, int y) {
		return x/(y+1) ;
	}

	public short divByZeroShort(short x, short y) {
		// short div will internally casted to div on int, we'll check if this
		// is handled too by MAZE
		return (short) (x/(y+1)) ;
	}
	
	public long remByZero(long x, long y) {
		return x/(y+1) ;
	}
	
	public int nullDerefInteger(Integer x) {
		// Maze has an issue to generate a non-null Integer, possibly
		// because it calls a wrong constructor Integr(str).
		// TODO
		return x+1 ;
	}

	
	public int nullDerefString(String x) {
		// MAZE cannot generate null string
		// TODO
		return x.length() ;
	}
	
	public String nullDerefObject(Object x) {
		return x.toString() ;
	}
	
}