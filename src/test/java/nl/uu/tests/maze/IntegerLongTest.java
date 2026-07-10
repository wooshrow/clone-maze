package nl.uu.tests.maze;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import nl.uu.maze.analysis.JavaAnalyzer;
import nl.uu.maze.execution.DSEController;
import nl.uu.maze.main.cli.MazeCLI;
import nl.uu.maze.util.Z3ContextProvider;
import nl.uu.tests.maze.CUTs.CUT_IntegerLong;
import picocli.CommandLine;

public class IntegerLongTest {
	
	String binClassesDir = "./target/test-classes" ;
	String outputDir = "./tmp" ;	
	Class CUT     = CUT_IntegerLong.class ;
	String sp = " " ;
	
	LoggerInterceptor interceptor ;
	
	@BeforeEach
	void setup() {
		// make the JavaAnalyzer to drop its current instance, to force a fresh one
		// to be created:
		JavaAnalyzer.dropInstance();
		
		// setting logger interceptor:
		Logger logger = (Logger) LoggerFactory.getLogger(DSEController.class);
		this.interceptor = new LoggerInterceptor() ;
		interceptor.start(); 
		logger.addAppender(interceptor);
		logger.setLevel(Level.INFO);	
		
		// remove the output-test-file produced by MAZE:
		TestUtils.removeFile(Path.of(outputDir, CUT.getSimpleName() + "Test.java"));
	}
	
	
	@Test
	void test_xxx_flow() throws IOException {

		String argz =   "--classpath=" + binClassesDir
				      + sp + "--classname=" + CUT.getName()
				      + sp + "--output-path=" + outputDir 
				      + sp + "--do-not-close-z3-context=true" // don't close z3 context, or else the next tests will crash
				      + sp + "--constrain-FP-params-to-normal-numbers=true"
				      + sp
				      ;
	    int exitCode = new CommandLine(new MazeCLI()).execute(argz.split(" ") );
	    
	    //assertTrue(interceptor.anyMatch(msg -> msg.contains("#generated") && msg.contains("8"))) ;
	    
	    var outputFile = new TxtFileContent(Path.of(outputDir, CUT.getSimpleName() + "Test.java")) ;
	    
	    
	    /*
	    assertEquals(1, outputFile.countMatchingLines(z -> ! Preds.isCommentLine(z) 
	    		&& Preds.isAssertThrowsLine(ArrayIndexOutOfBoundsException.class,z))) ;

	    // div-by-zero option is not enabled so this should give zero:
	    assertEquals(0, outputFile.countMatchingLines(z -> ! Preds.isCommentLine(z) 
	    		&& Preds.isAssertThrowsLine(ArithmeticException.class,z))) ;
	    
	    assertEquals(2, outputFile.countMatchingLines(z -> ! Preds.isCommentLine(z) 
	    		&& Preds.isAssertThrowsLine(NullPointerException.class,z))) ;
*/
	    
	}

}
