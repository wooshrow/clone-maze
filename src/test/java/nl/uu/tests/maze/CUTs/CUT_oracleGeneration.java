package nl.uu.tests.maze.CUTs;

/**
 * CUT to test MAZE's functionality to generate oracles, normal and exceptional.
 */
public class CUT_oracleGeneration {
	
	public int methodWithNormalTermination(int x) {
		return x ;
	}
	
	public int methodThrowsExpectException(int x) throws ArithmeticException {
		x++ ;
		throw new ArithmeticException("I always throws ArithException.") ;
	}
	
	public int methodThrowsIllegalArgumentExcpetion(int x) {
		x++ ;
		throw new IllegalArgumentException("I refuse any x!") ;
	}
	
	public int methodThrowsUnexpectedException(int x) {
		x++ ;
		throw new Error("I always throws Error!") ;
	}
	
}