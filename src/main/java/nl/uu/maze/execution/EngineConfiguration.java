package nl.uu.maze.execution;

import java.util.Random;

import picocli.CommandLine.Option;

/**
 * Contains configuration that influences how the DSE engine works.
 * We will maintain the configuration globally with a Singleton pattern.
 * <p>
 */
public class EngineConfiguration {
	
	/**
	 * If true, the symbolic solver will add a constraint to every method
	 * parameter x of floating-number type, that it should be a normal number
	 * (so, not infinity nor NaN).
	 * 
	 * <p>Default: false.
	 */
	public boolean constrainFPNumberParametersToNormalNumbers = false ;
	
	/**
	 * When true generated regression oracles in the test-cases will be commented out.
	 * 
	 * <p>default: false.
	 */
    public boolean surpressRegressionOracles = false ;

    /**
     * When true, when a test throws an exception that is not declared as expected 
     * exception by the method under test will be propagated. So, it will not be asserted as 
     * an expected exception by the test oracle. Note that this means the test will 
     * then fail (a potential bug is found by Maze).
     * 
     * <p>default: false.
     */
    public boolean propagateUnexpectedExceptions = false ;
    
    /**
     * When true, MAZE will actively check expressions of the form x/y and x%y, whether a
     * division or remainder by zero error is possible. 
     * 
     * <p>Note that such an error can only happen on
     * types int or long. In Java, x/0 in float results in Infinity or NaN (when x is 0)
     * when the types are float-like.
     * For other integral-type like short, there is no separate / or % operator in Java bytecode.
     * The arguments will be up-casted to e.g. int (so, the / or % will be of int type).
     * 
     * <p>Default: false.
     */
    public boolean enableDivisionByZeroChecking = false ;
    
    /**
     * When true, MAZE will generate random values for parameters of the constructor
     * and method under tests. This is only applicable in the test generation through
     * concrete-driven symbolic execution, where some parameters may be left unconstrained
     * by the symbolic constrain solving part.
     * 
     * <p>Default: false. In which case unconstrained parameters are always instantiated
     * to their default value. E.g. int-type parameter to 0. 
     * 
     */
    public boolean randomSeedingInConcreteDriven = false ;
    
    /**
     * Global random seed. All random generators in MAZE should use this seed, if it is not
     * null.
     * <p>Default: null.
     */
    public Long globalRandomSeed = null ;
    
    /**
     * Normally MAZE will explore all possible execution paths (within the given depth bounding).
     * However, if this flag is true, only tests that contribute to new coverage will be generated.
     * A test generates "new coverage" if the test executes a statement/instruction or a branch
     * that was not executed by all the tests before it. Only coverage over target methods is counted.
     * E.g. coverage over private methods is not counted.
     * <p>Default: false.
     */
    public boolean minimalisticTestSuite = false ;
    
    /**
     * Get a fresh random generator, using {@link #globalRandomSeed} as the seed, if it is
     * defined. Else unseeded random generator is returned.
     */
    public Random getRandomGenerator() {
    	if (globalRandomSeed == null)
    		return new Random() ;
    	else 
    		return new Random(globalRandomSeed) ;
    }
	
	private EngineConfiguration() {
		
	}
	
	static private EngineConfiguration theEngineConfiguration ;
	
	static public synchronized EngineConfiguration getInstance() {
		if (theEngineConfiguration == null) {
			theEngineConfiguration = new EngineConfiguration() ;
		}
 		return theEngineConfiguration ;
	}

}
