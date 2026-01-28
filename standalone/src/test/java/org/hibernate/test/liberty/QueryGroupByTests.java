package org.hibernate.test.liberty;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JpaComplianceSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author Steve Ebersole
 */
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("complianceValues")
@DomainModel(annotatedClasses = {QueryGroupByTests.Person.class, QueryGroupByTests.Name.class})
@SessionFactory
public class QueryGroupByTests implements ServiceRegistryProducer {
	public static List<Boolean> complianceValues() {
		return List.of( FALSE, TRUE );
	}

	private final boolean complianceEnabled;

	public QueryGroupByTests(boolean complianceEnabled) {
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
	@AnalysisItem( id=17, feature = "JPQL GROUP BY (multiple fields)", description = "Grouping on fields not in SELECT",
			behavioralDifference = "EclipseLink allows grouping on non-selected fields; Hibernate enforces strict compliance")
	@AnalysisItem( id=18, feature = "JPQL GROUP BY (embedded fields)", description = "Grouping with embedded paths",
			behavioralDifference = "EclipseLink supports embedded grouping; Hibernate may fail if path is ambiguous")
	void testGroupByEmbedded(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "select count(p.id) from Person p group by p.name" ).list();
			session.createQuery( "select count(p.id) from Person p group by p.name.firstName, p.name.lastName" ).list();
		} );
	}

	@Test
	@AnalysisItem( id=17, feature = "JPQL GROUP BY (multiple fields)", description = "Grouping on fields not in SELECT",
			behavioralDifference = "EclipseLink allows grouping on non-selected fields; Hibernate enforces strict compliance")
	void testGroupByNonSelected(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "select count(*) from Person p group by p.dob, p.name.lastName" ).list();
		} );
	}

	@Test
	@AnalysisItem( id=19, feature = "JPQL HAVING (non-aggregated fields)", description = "Using HAVING without aggregates",
			behavioralDifference = "EclipseLink allows non-aggregated HAVING; Hibernate enforces aggregate-only")
	void testHavingNoAggregation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "select p.dob, p.name.lastName, count(*) from Person p group by p.dob, p.name.lastName having p.dob > :dob" )
					.setParameter( "dob", Instant.now() )
					.list();
			session.createQuery( "select p.dob, p.name.lastName from Person p group by p.dob, p.name.lastName having p.dob > :dob" )
					.setParameter( "dob", Instant.now() )
					.list();
		} );
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		@Embedded
		@NaturalId
		private Name name;
		private Instant dob;

		public Person() {
		}

		public Person(Integer id, Name name, Instant dob) {
			this.id = id;
			this.name = name;
			this.dob = dob;
		}

		public Integer getId() {
			return id;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public Instant getDob() {
			return dob;
		}

		public void setDob(Instant dob) {
			this.dob = dob;
		}
	}

	@Embeddable
	public static class Name {
		private String firstName;
		private String lastName;

		public Name() {
		}

		public Name(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}
}
