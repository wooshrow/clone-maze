package nl.uu.maze.search.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import nl.uu.maze.execution.symbolic.CoverageTracker;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.execution.symbolic.TargetPath.TargetPathStatus;

public class PathCoverSearch extends SearchStrategy<SymbolicState> {
	
	static int ordering(SymbolicState S1, SymbolicState S2) {
		return Float.compare(S1.getTargetPath().hdist, S2.getTargetPath().hdist) ;
	}
	
	private PriorityQueue<SymbolicState> targetCovered = new PriorityQueue<>((S1,S2) -> ordering(S1,S2)) ;
	private PriorityQueue<SymbolicState> partiallyCovered = new PriorityQueue<>((S1,S2) -> ordering(S1,S2)) ;
	private PriorityQueue<SymbolicState> approaching = new PriorityQueue<>((S1,S2) -> ordering(S1,S2)) ;
	private PriorityQueue<SymbolicState> haveNoTarget = new PriorityQueue<>((S1,S2) -> ordering(S1,S2)) ;
	
	@Override
	public String getName() {
		return "PathCoverSearch" ;
	}


	@Override
    public boolean requiresPathTargetingAndTracking() {
        return true;
    }

	@Override
	public void add(SymbolicState target) {
		switch (target.getTargetPath().status) {
		  case TARGET_COVERED : targetCovered.offer(target) ; break ;
		  case TARGET_PARTIALLY_COVERED : partiallyCovered.offer(target) ; break ;		
		  case APPROACHING_TARGET : approaching.offer(target) ; break ;
		  case HAS_NO_TARGET : haveNoTarget.offer(target) ; break ;
		}
		count++ ;
	}
	

	@Override
	public void remove(SymbolicState target) {
		boolean changed = targetCovered.remove(target) ;
		if (changed) return ;
		changed = partiallyCovered.remove(target) ;
		if (changed) return ;
		changed = approaching.remove(target) ;
		if (changed) return ;
		haveNoTarget.remove(target) ;		
	}

	
	
	@Override
	public SymbolicState next() {
		CoverageTracker cov = CoverageTracker.getInstance() ;
		if (cov.isDirty()) {
			System.out.println("### PCS recalculating targets...") ;
			List<SymbolicState> tmp = new LinkedList<>() ;
			Collection<SymbolicState> covered = new LinkedList<>() ;
			Collection<SymbolicState> partial = new LinkedList<>() ;
			Collection<SymbolicState> approaching_ = new LinkedList<>() ;
			Collection<SymbolicState> noTarget_ = new LinkedList<>() ;
			
			if (! targetCovered.isEmpty()) {
				tmp.addAll(targetCovered) ;
				for (var S : tmp) {
					S.updateTargetPathStatus();
					if (S.getTargetPath().status != TargetPathStatus.TARGET_COVERED) {
						targetCovered.remove(S) ;
						add_(S,covered,partial,approaching_,noTarget_) ;
					}
				}
			}
			
			if (! partiallyCovered.isEmpty()) {
				tmp.clear();
				tmp.addAll(partiallyCovered) ;
				for (var S : tmp) {
					S.updateTargetPathStatus();
					if (S.getTargetPath().status != TargetPathStatus.TARGET_PARTIALLY_COVERED) {
						partiallyCovered.remove(S) ;
						add_(S,covered,partial,approaching_,noTarget_) ;
					}
				}
			}
			
			if (! approaching.isEmpty()) {
				tmp.clear();
				tmp.addAll(approaching) ;
				for (var S : tmp) {
					S.updateTargetPathStatus();
					if (S.getTargetPath().status != TargetPathStatus.APPROACHING_TARGET) {
						approaching.remove(S) ;
						add_(S,covered,partial,approaching_,noTarget_) ;
					}
				}
			}
			
			int k = covered.size() + partial.size() + approaching_.size() + noTarget_.size() ;
			System.out.println("### re-targeting " + k) ;

			targetCovered.addAll(covered) ;
			partiallyCovered.addAll(partial) ;
			approaching.addAll(approaching_) ;
			haveNoTarget.addAll(noTarget_) ;
			
			cov.cleanDirtyFlag() ;
		}
		
		
		SymbolicState S = targetCovered.poll() ;
		if (S == null) 
			S = partiallyCovered.poll() ;
		if (S == null) 
			S = approaching.poll() ;
		if (S == null) 
			S = haveNoTarget.poll() ;
		if (S != null && S.getTargetPath().status == TargetPathStatus.TARGET_COVERED) {
			/*
			System.out.println(">>> PCS next ") ;
			System.out.println("    stmt: " + S.getStmt()) ;
			System.out.println("    target: " + S.getTargetPath().targetpath + ", " + S.getTargetPath().status) ;
			System.out.println("    bhist : " + S.getBranchHistory()) ;
			System.out.println("    HDIST : " +  S.getTargetPath().hdist) ;	
			*/
		}
		return S ;
	}

	private void add_(SymbolicState S, 
			Collection<SymbolicState> covered,
			Collection<SymbolicState> partial,
			Collection<SymbolicState> approaching_ ,
			Collection<SymbolicState> noTarget_
			) {
		switch (S.getTargetPath().status) {
		  case TARGET_COVERED : covered.add(S) ; break ;
		  case TARGET_PARTIALLY_COVERED : partial.add(S) ; break ;		
		  case APPROACHING_TARGET : approaching_.add(S) ; break ;
		  case HAS_NO_TARGET : noTarget_.add(S) ; break ;
		}
	}
	
	@Override
	public int size() {
		return targetCovered.size() 
			+ partiallyCovered.size() 
			+ approaching.size() 
			+ haveNoTarget.size() ;
	}

	@Override
	public void reset() {
		targetCovered.clear() ;
		partiallyCovered.clear() ;
		approaching.clear() ;
		haveNoTarget.clear() ;
	}

	@Override
	public Collection<SymbolicState> getAll() {
		List<SymbolicState> U = new LinkedList<>() ;
		U.addAll(targetCovered) ;
		U.addAll(partiallyCovered) ;
		U.addAll(approaching) ;
		U.addAll(haveNoTarget) ;
		return U ;
	}

}
