package nl.uu.tests.maze.CUTs;

/**
 * CUT for testing MAZE ability to generate normal and special float values.
 */
public class CUT_FloatValuesGeneration {
	
	public double return_0_when_EquiDistTriangle(double x, double y, double z) {
		if (x+y>z && y+z>x && x+z>y && x==y && y==z)
			return 0 ;
		return x ;
	}
	
	public double return_posInfinity(double x) {
		if (x == Double.POSITIVE_INFINITY)
			// MAZE cannot solve this! TODO.
			return x ;
		return 0 ;
	}
	
	public float return_x_when_negInfinity(float x) {
		if (x == Float.NEGATIVE_INFINITY)
			// MAZE cannot solve this! TODO.
			return x ;
		return 0 ;
	}	
	
	public double return_NaN(double x) {
		if (x <= Double.POSITIVE_INFINITY)
			return 0 ;
		return x ;
	}
}