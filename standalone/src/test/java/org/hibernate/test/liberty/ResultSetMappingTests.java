package org.hibernate.test.liberty;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = Book.class)
@SessionFactory
public class ResultSetMappingTests {

	@Test
	@AnalysisItem( id = 13, feature = "Native query result mapping", description = "Using resultClass without full mapping",
			behavioralDifference = "EclipseLink accepts partial mapping; Hibernate requires full match")
	void testNativeQuerySimpleResultClass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final List<Book> books = session.createNativeQuery( "select * from books", Book.class ).list();
			assertThat( books ).hasSize( 1 );
			assertThat( books.getFirst().getId() ).isEqualTo( 1 );
			assertThat( books.getFirst().getTitle() ).isEqualTo( "Pet Cemetery" );
		} );
	}

	@Test
	@AnalysisItem( id = 13, feature = "Native query result mapping", description = "Using resultClass without full mapping",
			behavioralDifference = "EclipseLink accepts partial mapping; Hibernate requires full match")

	void testPartialResultClassMapping(SessionFactoryScope factoryScope) {
		// test a partial mapping
		factoryScope.inTransaction( (session) -> {
			final List<Book> books = session.createNativeQuery( "select id, title, isbn from books", Book.class ).list();
			assertThat( books ).hasSize( 1 );
			assertThat( books.getFirst().getId() ).isEqualTo( 1 );
			assertThat( books.getFirst().getTitle() ).isEqualTo( "Pet Cemetery" );
		} );
	}

	@Test
	@AnalysisItem( id = 14, feature = "@NamedNativeQuery result mapping", description = "Native query with resultClass",
			behavioralDifference = "EclipseLink accepts flexible mapping; Hibernate requires exact match")
	void testNamedNativeQueryResultClass(SessionFactoryScope factoryScope) {
		// first, with a specified resultClass
		factoryScope.inTransaction( (session) -> {
			final List<Book> books = session.createNamedQuery( "book-complete", Book.class ).list();
			assertThat( books ).hasSize( 1 );
			assertThat( books.getFirst().getId() ).isEqualTo( 1 );
			assertThat( books.getFirst().getTitle() ).isEqualTo( "Pet Cemetery" );
		} );

		// then, with the resultClass from the annotation
		factoryScope.inTransaction( (session) -> {
			final List<Book> books = session.createNamedQuery( "book-complete" ).list();
			assertThat( books ).hasSize( 1 );
			assertThat( books.getFirst().getId() ).isEqualTo( 1 );
			assertThat( books.getFirst().getTitle() ).isEqualTo( "Pet Cemetery" );
		} );
	}

	@Test
	@AnalysisItem( id = 16, feature = "@SqlResultSetMapping mismatch", description = "Mapping native query results to entity",
			behavioralDifference = "EclipseLink tolerates mismatch; Hibernate throws if mapping is off")
	void testResultSetMappingWithUnderdefinedEntityResult(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final List<Book> books = session.createNativeQuery( "select * from books", "book-complete" ).list();
			assertThat( books ).hasSize( 1 );
			assertThat( books.getFirst().getId() ).isEqualTo( 1 );
			assertThat( books.getFirst().getTitle() ).isEqualTo( "Pet Cemetery" );
		} );
	}

	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Book( 1, "Pet Cemetery", "Stephen King", "123-45-6789" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

}
