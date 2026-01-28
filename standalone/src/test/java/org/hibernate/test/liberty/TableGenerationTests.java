package org.hibernate.test.liberty;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/// May also be used to check id-36, though waiting on clarification
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = { TableGenerationTests.Things.class, TableGenerationTests.MoreThings.class})
@SessionFactory
public class TableGenerationTests {
	/// Turns out this is about creating the id table *without* schema generation - iow, as the EMF is
	/// bootstrapped, EclipseLink will generate the id table (I'd assume sequences too) always if needed.
	///
	/// Need to decide if this is something we want to do.
	@Test
	@AnalysisItem(id = 2, feature = "@GeneratedValue(TABLE)", description = "Table-based ID generation without generator",
			behavioralDifference = "EclipseLink auto-generates table; Hibernate fails without explicit generator")
	void testJustGeneratedValue(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Things( "stuff" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity
	@Table(name="things")
	public static class Things {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Integer id;
		private String name;

		public Things() {
		}

		public Things(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="MoreThings")
	@Table(name="more_things")
	public static class MoreThings {
		@Id
		@TableGenerator(name = "more_things_ids")
		private Integer id;
		private String name;
	}

}
