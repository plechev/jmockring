package org.jmockring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jmockring.webserver.WebServer;

/**
 * Declare default values at class level.
 * <p/>
 * This helps avoid repeating the configuration settings in the annotations used to inject remote resources into the test class, such as
 * {@link RemoteBean}, {@link RemoteMock}, {@link RemoteSpring}, {@link RemoteRequestListener}, etc ...
 *
 * @author Pavel Lechev
 * @date 21/04/13
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextDefaults {

    String contextPath();

    Class<? extends WebServer> bootstrap();

    String executionName() default Server.DEFAULT_EXECUTION_NAME;

    @ContextDefaults(contextPath = "", bootstrap = WebServer.class, executionName = Server.DEFAULT_EXECUTION_NAME)
    final class DEFAULTS {

        public static final ContextDefaults get() {
            return DEFAULTS.class.getAnnotation(ContextDefaults.class);
        }
    }

}
