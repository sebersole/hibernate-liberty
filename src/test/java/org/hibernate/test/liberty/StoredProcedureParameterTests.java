package org.hibernate.test.liberty;

import java.sql.Statement;

import org.hibernate.procedure.ProcedureCall;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = Book.class)
@SessionFactory
public class StoredProcedureParameterTests {
	@Test
	void testCompliantProcedureCall(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final ProcedureCall spCountBooks = session.createStoredProcedureQuery( "sp_count_books" );
			spCountBooks.registerParameter( 1, String.class, ParameterMode.IN );
			spCountBooks.registerParameter( 2, int.class, ParameterMode.OUT );
			spCountBooks.setParameter( 1, "Stephen King" );
			spCountBooks.execute();
			spCountBooks.getParameter( 2 );
		} );
	}

	@Test
	@AnalysisItem( id = 32, feature = "Stored Procedures INOUT/OUT", description = "Parameter mode handling",
			behavioralDifference = "EclipseLink auto-detects mode; Hibernate needs manual declaration")
	@FailureExpected(reason = "This is actually something Hibernate does not support - not sure we do necessarily")
	void testProposedProcedureCall(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final ProcedureCall spCountBooks = session.createStoredProcedureQuery( "sp_count_books" );
			spCountBooks.setParameter( 1, "Stephen King" );
			spCountBooks.execute();
			spCountBooks.getParameter( 2 );
		} );
	}

	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Book( 1, "Pet Cemetery", "Stephen King", "123-45-6789" ) );
			session.doWork( (connection) -> {
				try (Statement st = connection.createStatement()) {
					st.execute(
							"""
									create procedure sp_count_books
										@author varchar,
										@bookCount int output
									as
									begin
										select @bookCount = count(1)
										from books
										where author = @author
									END
									"""
					);
				}
			} );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		factoryScope.inTransaction( (session) -> session.doWork(  (connection) -> {
			try (Statement st = connection.createStatement()) {
				st.execute( "drop procedure sp_count_books" );
			}
		} ) );
	}
}
