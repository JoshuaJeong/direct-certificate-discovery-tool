package gov.hhs.onc.dcdt.beans;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Phase {
    public final static int PHASE_PRECEDENCE_HIGHEST = 0;
    public final static int PHASE_PRECEDENCE_LOWEST = Integer.MAX_VALUE;
    
    int value();
}
