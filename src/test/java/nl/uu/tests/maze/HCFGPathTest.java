package nl.uu.tests.maze;

import org.junit.jupiter.api.Test;

import nl.uu.maze.util.HCFG;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

public class HCFGPathTest {
	
	
	List<Integer> toList(int[] s) {
		List<Integer> ss = new LinkedList<>() ;
		for (int k = 0 ; k<s.length; k++) {
			ss.add(s[k]) ;
		}
		return ss ;
	}
	
	@Test
	public void test_cover() {	
		int[] hist = {1, 2, 4, 2, 3, 2, 3, 5, 1} ;
		int[] sigma = {2, 3, 5} ;
		assertTrue(HCFG.cover(toList(hist), toList(sigma)) == 0) ;
	}
	
	@Test
	public void test_nocover() {
		int[] hist = {1, 2, 4, 2, 3, 2, 3, 1} ;
		int[] sigma = {2, 3, 5} ;
		assertTrue(HCFG.cover(toList(hist), toList(sigma)) == -1) ;
	}
	
	@Test
	public void test_partialcover1() {
		int[] hist = {1, 2, 4, 2, 1, 2, 3} ;
		int[] sigma = {2, 3, 5} ;
		assertTrue(HCFG.cover(toList(hist), toList(sigma)) == 1) ;
	}
	
	@Test
	public void test_partialcover2() {
		int[] hist = {1, 2, 4, 2, 3, 1, 2} ;
		int[] sigma = {2, 3, 5} ;
		assertTrue(HCFG.cover(toList(hist), toList(sigma)) == 2) ;
	}

}
