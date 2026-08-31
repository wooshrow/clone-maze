package nl.uu.maze.examples;

public class EX0_Check {
	public static void check(int[] a) {
		if (a != null)
			assert new EX0(a).isSorted() < a.length - 1;
	}
}
