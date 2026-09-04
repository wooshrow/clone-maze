package nl.uu.tests.coba;

import nl.uu.maze.main.Application;
import nl.uu.maze.main.cli.MazeCLI;
import picocli.CommandLine;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

// Just for trying out Maze-application, for convenience, invoked from here
public class CobaTargetToplevelCUT {
	
	@Test
	void coba() {
				
		String[] args_ = { "--help" } ;
		
		String cobabenchPath = "../my_simple_bench" ;
		String CUT = "CUTtoplevel" ;
		
		String sp = " " ;

		String argz =   "--classpath=" + cobabenchPath + "/target/classes"
				      + sp + "--classname=" + CUT 
				      + sp + "--indirectTarget=cobabench.SomeCUT"
				      + sp + "--output-path=" + cobabenchPath + "/src/test/java/"
				      + sp + "-s=BFS"
				      + sp + "--minimalistic-suite=true"
				      + sp + "-b=10"				      
				      + sp
				      ;

		args_ = argz.split(" ") ;
		
		// Application.main(args_);  --> this call System.exit() which causes Maven test runner to crash
		
		// we'll do this instead, which is what the main() above does:
        int exitCode = new CommandLine(new MazeCLI()).execute(args_);
    }

}
