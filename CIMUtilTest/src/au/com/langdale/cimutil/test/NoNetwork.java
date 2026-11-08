package au.com.langdale.cimutil.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs the annotated test class with HTTP and HTTPS routed through an
 * unreachable proxy, so that any attempt by a builder or the CLI to reach the
 * network fails the test instead of reaching the internet.
 *
 * <p>
 * CIMTool is used in utility environments where outbound network access during
 * a build is a security concern, so no test in CIMUtilTest should depend on it.
 * The proxy settings are applied before the class's first test, including any
 * {@code @BeforeAll} method, and restored after its last.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(NoNetworkExtension.class)
public @interface NoNetwork {
}
