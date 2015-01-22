package org.jmockring.javassist;

//import javassist.CannotCompileException;
//import javassist.ClassPool;
//import javassist.CtClass;
//import javassist.CtField;
//import javassist.CtMethod;
//import javassist.CtNewMethod;
//import javassist.NotFoundException;

/**
 * @author Pavel Lechev <plechev@cisco.com>
 * @since 02/09/2014
 */
public class Tryout {

//
//    private static final ClassPool classPool = ClassPool.getDefault();
//
//    private static final Logger log = LoggerFactory.getLogger(Tryout.class);
//
//    public static void main(String[] args) throws NotFoundException, CannotCompileException, MalformedURLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        redefineSpringContextLoader(ClassLoader.getSystemClassLoader());
//    }
//
//
//    public static void redefineSpringContextLoader(ClassLoader classLoader) throws NotFoundException, CannotCompileException, MalformedURLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        CtClass springClass = classPool.get("org.springframework.web.context.ContextLoader");
//        CtClass jmockringClass = classPool.get("org.jmockring.spring.ServerConfigurationAwareContextLoaderListener");
//
//        // replace method definitions
//        replaceMethodBody("createWebApplicationContext", springClass, jmockringClass);
//        replaceMethodBody("determineContextClass", springClass, jmockringClass);
//
//        // add fields:
//        addField("baseContextConfiguration", BaseContextConfiguration.class, springClass);
//        addField("serverConfiguration", ServerConfiguration.class, springClass);
//        addField("bootstrap", Class.class, springClass);
//
//        // load instrumented Spring class in JVM
//        springClass.toClass();
//
//        ContextLoader loader = new ContextLoader();
//
//        // initialise
//        setField("baseContextConfiguration", new WebAppContextConfiguration(null, null), BaseContextConfiguration.class, loader);
//        setField("serverConfiguration", new ServerConfiguration(null, null), ServerConfiguration.class, loader);
//        setField("bootstrap", TomcatWebServer.class, Class.class, loader);
//
//        loader.initWebApplicationContext(new JspCServletContext(IO.getNullPrintWriter(), new URL("http://www.test.com")));
//    }
//
//    /**
//     * @param fieldName
//     * @param initValue
//     * @param instance
//     * @throws NoSuchFieldException
//     * @throws IllegalAccessException
//     */
//    private static void setField(final String fieldName,
//                                 final Object initValue,
//                                 final Class initValueType,
//                                 final Object instance) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        ContextLoader.class.getMethod("set_" + fieldName, initValueType).invoke(instance, initValue);
//    }
//
//    /**
//     * @param methodName
//     * @param springClass
//     * @param jmockringClass
//     * @throws NotFoundException
//     * @throws CannotCompileException
//     */
//    private static void replaceMethodBody(final String methodName, final CtClass springClass, final CtClass jmockringClass) throws NotFoundException, CannotCompileException {
//        final CtMethod destM = springClass.getDeclaredMethod(methodName);
//        final CtMethod srcM = jmockringClass.getDeclaredMethod(methodName);
//        log.info("Replacing body of {} to {}", destM, srcM);
//        destM.setBody(srcM, null);
//    }
//
//    private static void addField(final String fieldName,
//                                 final Class fieldType,
//                                 final CtClass springClass) throws NotFoundException, CannotCompileException {
//        CtField field = new CtField(classPool.get(fieldType.getName()), fieldName, springClass);
//        springClass.addField(field);
//        springClass.addMethod(CtNewMethod.setter("set_" + fieldName, field));
//    }

}
