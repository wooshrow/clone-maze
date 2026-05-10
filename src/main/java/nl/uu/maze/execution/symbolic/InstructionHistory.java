package nl.uu.maze.execution.symbolic;

import java.util.LinkedList;
import java.util.List;

import sootup.core.jimple.common.stmt.Stmt;
import sootup.java.core.JavaSootMethod;

/**
 * Representing the sequence of instructions passed by an execution.
 */
public class InstructionHistory {
	
	public static abstract class InstructionHistoryItem { }
	
	public static class MethodSwitchItem extends InstructionHistoryItem {
		public JavaSootMethod method ;		
		MethodSwitchItem(JavaSootMethod m) { method = m ; }
		@Override
		public String toString() {
		   return "switch-to: " + method.getName()	;
		}
	}
	public static class InstructionItem extends InstructionHistoryItem {
		public Stmt stmt ;
		InstructionItem(Stmt s) { stmt = s ; }		
		@Override
		public String toString() {
		   return stmt.toString() ;
		}
	}
	
	List<InstructionHistoryItem> history = new LinkedList<>() ;
	
	public InstructionHistory() { }
	
	public void addMethodSwitch(JavaSootMethod m) {
		history.add(new MethodSwitchItem(m)) ;
	}
	
	public void addInstruction(Stmt stmt) {
		history.add(new InstructionItem(stmt)) ;
	}
	
	public List<InstructionHistoryItem> getHistory() {
		return history ;
	}
	
}
