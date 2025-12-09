package org.hibernate.test.liberty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/// Correlates tests to a corresponding assertion in the spreadsheet
///
/// @author Steve Ebersole
@Target({ ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable( Analysis.class )
public @interface AnalysisItem {
	/// `Id` column from spreadsheet
	int id();
	/// `Feature` column from spreadsheet
	String feature();
	/// `Description` column from spreadsheet
	String description();
	/// `JPA Team: Behavioral Difference Description` column from spreadsheet
	String behavioralDifference();
}
