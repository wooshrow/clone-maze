package nl.uu.maze.examples;

public class EX0 {
	
	int[] a ;
	
	public EX0(int[] a){ this.a = a ; }

	public int isSorted() {
		for (int k = 0; k < a.length - 1; k++)
			if (a[k] > a[k + 1])
				return k;
		return -1;
	}
}
