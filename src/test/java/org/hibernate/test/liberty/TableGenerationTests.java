package org.hibernate.test.liberty;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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

import static org.assertj.core.api.Assertions.assertThat;

/// May also be used to check id-36, though waiting on clarification
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {TableGenerationTests.Thing1.class})
@SessionFactory
public class TableGenerationTests {
	@Test
	@AnalysisItem(id = 2, feature = "@GeneratedValue(TABLE)", description = "Table-based ID generation without generator",
			behavioralDifference = "EclipseLink auto-generates table; Hibernate fails without explicit generator")
	void testJustGeneratedValue(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Thing1( "stuff" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity
	@Table(name="thing1s")
	public static class Thing1 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Integer id;
		private String name;

		public Thing1() {
		}

		public Thing1(String name) {
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

}
