package nl.uu.maze.model;

/**
 * An object-factory is a method that creates an object of a certain type. Constructors
 * are such a factory, but it could also be an ordinary method that takes the role of
 * a constructor. This is a common pattern in OO.
 * 
 * <p>When it is called from inside a method under test, for MAZE this not much different
 * from an ordinary method call. So, we can also have a model for such an object-constructor
 * method.
 * 
 * <p>However, when an object-factory is also used, or needs to be used....h 
 */
public abstract class ModelOfObjectFactory extends AbsModelOfMethod {

}
