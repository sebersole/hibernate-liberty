package org.hibernate.test.liberty;

import java.sql.Statement;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = Book.class)
@SessionFactory
public class QueryFunctionTests {
	@Test
	@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
	void testSelectionHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.createQuery( "select id, dbo.constant_number() from Book", Tuple.class ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
			assertThat( result.getFirst().get( 1 ) ).isEqualTo( 1 );
		} );
	}

	@Test
	@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
	void testSelectionHql2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.createQuery( "select id, dbo.multiplied_number(2) from Book", Tuple.class ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
			assertThat( result.getFirst().get( 1 ) ).isEqualTo( 4 );
		} );
	}

	@Test
	@AnalysisItem( id = 24, feature = "JPQL FUNCTION(...)", description = "Dialect-specific function support",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs explicit registration")
	void testSelectionHql3(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.createQuery( "select id, function( 'dbo.multiplied_number' as integer, 2 ) from Book", Tuple.class ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
			assertThat( result.getFirst().get( 1 ) ).isEqualTo( 4 );
		} );
	}

	@Test
	void testSelectionCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Tuple.class );
			var func = criteriaBuilder.function( "dbo.constant_number", Integer.class );
			var root = criteria.from( Book.class );
			criteria.select( criteriaBuilder.tuple( root.get( "id" ), func ) );

			var result = session.createQuery( criteria ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
			assertThat( result.getFirst().get( 1 ) ).isEqualTo( 1 );
		} );
	}

	@Test
	void testSelectionCriteria2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Tuple.class );
			var func = criteriaBuilder.function( "dbo.multiplied_number", Integer.class, criteriaBuilder.literal( 2 ) );
			var root = criteria.from( Book.class );
			criteria.select( criteriaBuilder.tuple( root.get( "id" ), func ) );

			var result = session.createQuery( criteria ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
			assertThat( result.getFirst().get( 1 ) ).isEqualTo( 4 );
		} );
	}

	@Test
	@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
	void testOrderByHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book order by dbo.constant_number()" ).list();
		} );
	}

	@Test
	@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
	void testOrderByHql2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book order by dbo.multiplied_number(2)" ).list();
		} );
	}

	@Test
	@AnalysisItem( id = 24, feature = "JPQL FUNCTION(...)", description = "Dialect-specific function support",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs explicit registration")
	void testOrderByHql3(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book order by function( 'dbo.multiplied_number' as int, 2)" ).list();
		} );
	}

	@Test
	void testOrderByCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.constant_number", Integer.class );
			criteria.from( Book.class );
			criteria.orderBy( criteriaBuilder.asc( func ) );

			session.createQuery( criteria ).list();
		} );
	}

	@Test
	void testOrderByCriteria2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.multiplied_number", Integer.class, criteriaBuilder.literal( 2 ) );
			criteria.from( Book.class );
			criteria.orderBy( criteriaBuilder.asc( func ) );

			session.createQuery( criteria ).list();
		} );
	}

	@Test
	@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
	@FailureExpected(reason = "One of the (few) places where this will fail - Hibernate wants to check the comparison types")
	void testComparisonHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book where id = dbo.constant_number()" ).list();
		} );
	}

	@Test
	@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
	@FailureExpected(reason = "One of the (few) places where this will fail - Hibernate wants to check the comparison types")
	void testComparisonHql2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book where id = dbo.multiplied_number(2)" ).list();
		} );
	}

	@Test
	@AnalysisItem( id = 24, feature = "JPQL FUNCTION(...)", description = "Dialect-specific function support",
			behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs explicit registration")
	void testComparisonHql3(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book where id = function( 'dbo.multiplied_number' as int, 2)" ).list();
		} );
	}

	@Test
	void testComparisonCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.constant_number", Integer.class );
			var root = criteria.from( Book.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), func ) );

			session.createQuery( criteria ).list();
		} );
	}

	@Test
	void testComparisonCriteria2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.multiplied_number", Integer.class, criteriaBuilder.literal( 2 ) );
			var root = criteria.from( Book.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), func ) );

			session.createQuery( criteria ).list();
		} );
	}

	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Book( 1, "Pet Cemetery", "Stephen King", "123-45-6789" ) );

			session.doWork( (connection) -> {
				try (Statement statement = connection.createStatement() ) {
					statement.execute(
							"""
								create function constant_number()
								returns int
								as
								begin
									return 1
								end
								"""
					);
					statement.execute(
							"""
								create function multiplied_number(
								    @base int
								)
								returns int
								as
								begin
									return @base *2
								end
								"""
					);
				}
			} );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (Statement statement = connection.createStatement() ) {
				statement.execute( "drop function constant_number" );
				statement.execute( "drop function multiplied_number" );
			}
		} ) );
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
