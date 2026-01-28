package org.hibernate.test.liberty;

import java.sql.Statement;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Tuple;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/// Also an analysis of id-31 which is a general case of function use.
///
/// @implNote This actually, I think, comes down to Hibernate's JpaCompliance.
/// Still waiting on confirmation of that.
/// Assuming that is the case, the workaround is to simply disable query compliance.
///
/// @author Steve Ebersole
@AnalysisItem( id = 24, feature = "JPQL FUNCTION(...)", description = "Dialect-specific function support",
		behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs explicit registration")
@AnalysisItem( id = 31, feature = "DB Functions", description = "Dialect dependency",
		behavioralDifference = "EclipseLink accepts DB functions directly; Hibernate needs dialect support")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("complianceValues")
@DomainModel(annotatedClasses = Book.class)
@SessionFactory
public class QueryFunctionTests implements ServiceRegistryProducer {
	public static List<Boolean> complianceValues() {
		return List.of( FALSE, TRUE );
	}

	private final boolean complianceEnabled;

	public QueryFunctionTests(boolean complianceEnabled) {
		this.complianceEnabled = complianceEnabled;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder standardServiceRegistryBuilder) {
		return new StandardServiceRegistryBuilder()
				.applySetting( JpaComplianceSettings.JPA_QUERY_COMPLIANCE, complianceEnabled )
				.build();
	}

	@Override
	public void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder) {
		// nothing to do
	}

	@Test
	void testCompliantSelection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.createQuery( "select id, function( 'dbo.multiplied_number' as integer, 2 ) from Book", Tuple.class ).list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
			assertThat( result.getFirst().get( 1 ) ).isEqualTo( 4 );
		} );
	}

	@Test
	void testFunctionNoArgSelectionCriteria(SessionFactoryScope factoryScope) {
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
	void testFunctionArgSelectionCriteria(SessionFactoryScope factoryScope) {
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
	void testNoArgFunctionSelection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				var result = session.createQuery( "select id, dbo.constant_number() from Book", Tuple.class ).list();
				assertThat( result ).hasSize( 1 );
				assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
				assertThat( result.getFirst().get( 1 ) ).isEqualTo( 1 );

				if ( complianceEnabled ) {
					fail( "Expecting a failure as compliance was enabled" );
				}
			}
			catch (IllegalArgumentException e) {
				assertThat( complianceEnabled ).isTrue();
				assertThat( e.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
			}
			catch (StrictJpaComplianceViolation complianceViolation) {
				assertThat( complianceEnabled ).isTrue();
			}
		} );
	}

	@Test
	void tryArgFunctionSelection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				var result = session.createQuery( "select id, dbo.multiplied_number(2) from Book", Tuple.class ).list();
				assertThat( result ).hasSize( 1 );
				assertThat( result.getFirst().get( 0 ) ).isEqualTo( 1 );
				assertThat( result.getFirst().get( 1 ) ).isEqualTo( 4 );

				if ( complianceEnabled ) {
					fail( "Expecting a failure as cpompliance was enabled" );
				}
			}
			catch (IllegalArgumentException e) {
				assertThat( complianceEnabled ).isTrue();
				assertThat( e.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
			}
			catch (StrictJpaComplianceViolation complianceViolation) {
				// we expect this with compliance enabled
				assertThat( complianceEnabled ).isTrue();
			}
		} );
	}

	@Test
	void testCompliantOrderBy(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// just making sure it executes correctly...
			session.createQuery( "select b from Book b order by function( 'dbo.constant_number' as int)" ).list();
			session.createQuery( "select b from Book b order by function( 'dbo.multiplied_number' as int, 2)" ).list();
		} );
	}

	@Test
	void testOrderByNoArgFunctionCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// just making sure it executes correctly...
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.constant_number", Integer.class );
			criteria.from( Book.class );
			criteria.orderBy( criteriaBuilder.asc( func ) );

			session.createQuery( criteria ).list();
		} );
	}

	@Test
	void testOrderByArgFunctionCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// just making sure it executes correctly...
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.multiplied_number", Integer.class, criteriaBuilder.literal( 2 ) );
			criteria.from( Book.class );
			criteria.orderBy( criteriaBuilder.asc( func ) );

			session.createQuery( criteria ).list();
		} );
	}

	@Test
	void testOrderByNoArgFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "select b from Book b order by dbo.constant_number()" ).list();
				if ( complianceEnabled ) {
					fail( "Expecting a compliance failure" );
				}
			}
			catch (IllegalArgumentException iae) {
				assertThat( complianceEnabled ).isTrue();
				assertThat( iae.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
			}
			catch (StrictJpaComplianceViolation complianceViolation) {
				assertThat( complianceEnabled ).isTrue();
			}
		} );
	}

	@Test
	void testOrderByArgFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "select b from Book b order by dbo.multiplied_number(2)" ).list();
				if ( complianceEnabled ) {
					fail( "Expecting a compliance failure" );
				}
			}
			catch (IllegalArgumentException iae) {
				assertThat( complianceEnabled ).isTrue();
				assertThat( iae.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
			}
			catch (StrictJpaComplianceViolation complianceViolation) {
				assertThat( complianceEnabled ).isTrue();
			}
		} );
	}

	@Test
	void testCompliantComparison(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "select b from Book b where b.id = function( 'dbo.multiplied_number' as int, 2)" ).list();
		} );
	}

	@Test
	void testNoArgFunctionComparisonCriteria(SessionFactoryScope factoryScope) {
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
	void testArgFunctionComparisonCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Book.class );
			var func = criteriaBuilder.function( "dbo.multiplied_number", Integer.class, criteriaBuilder.literal( 2 ) );
			var root = criteria.from( Book.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), func ) );

			session.createQuery( criteria ).list();
		} );
	}

	/// Tests restrictions based on an unregistered function with no args.
	///
	/// @implNote This is not allowed as Hibernate wants to validate the
	/// left- and right-hand sides for "compatibility".
	@Test
	void testNoArgFunctionComparison(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "select b from Book b where b.id = dbo.constant_number()" ).list();
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException e) {
				if ( complianceEnabled ) {
					assertThat( e.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
				}
				else {
					assertThat( e.getCause() ).isInstanceOf( SemanticException.class );
					assertThat( e.getCause().getMessage() ).startsWith( "Cannot compare " );
				}
			}
		} );
	}

	/// Tests restrictions based on an unregistered function with args.
	///
	/// @implNote This is not allowed as Hibernate wants to validate the
	/// left- and right-hand sides for "compatibility".
	@Test
	void testArgFunctionComparison(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "select b from Book b where b.id = dbo.multiplied_number(2)" ).list();
				fail( "Expecting a compliance failure" );
			}
			catch (IllegalArgumentException e) {
				if ( complianceEnabled ) {
					assertThat( e.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
				}
				else {
					assertThat( e.getCause() ).isInstanceOf( SemanticException.class );
					assertThat( e.getCause().getMessage() ).startsWith( "Cannot compare " );
				}
			}
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
		factoryScope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				try (Statement statement = connection.createStatement() ) {
					statement.execute( "drop function constant_number" );
					statement.execute( "drop function multiplied_number" );
				}
			} );

			session.getSessionFactory().getSchemaManager().truncateMappedObjects();
		} );
	}
}
