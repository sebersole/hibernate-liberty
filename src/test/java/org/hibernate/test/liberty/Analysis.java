package org.hibernate.test.liberty;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/// Grouping of one or more [AnalysisItem]
///
/// @author Steve Ebersole
@Target({ ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Analysis {
	AnalysisItem[] value();
}
