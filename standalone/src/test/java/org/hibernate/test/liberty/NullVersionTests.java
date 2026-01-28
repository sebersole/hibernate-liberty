package org.hibernate.test.liberty;

import java.util.Date;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = NullVersionTests.TheEntity.class)
@SessionFactory
public class NullVersionTests {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@AnalysisItem( id = 4, feature = "@Version field null", description = "@Version field null ",
			behavioralDifference = "EclipseLink allows null version; Hibernate expects initialized version")
	void testPersistingNullVersions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "first", null ) );
		} );

		// the null should have been converted to the seed value (zero) prior to the insert
		factoryScope.inTransaction( (session) -> {
			var entity = session.find( TheEntity.class, 1 );
			assertThat( entity.version ).isEqualTo( 0 );
		} );
	}

	@Test
	@AnalysisItem( id = 4, feature = "@Version field null", description = "@Version field null ",
			behavioralDifference = "EclipseLink allows null version; Hibernate expects initialized version")
	void testExistingNullVersions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "second", null ) );
		} );

		// set the version to null in the database
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement() ) {
				statement.executeUpdate( "update the_entity set version = null" );
			}
		} ) );

		// now try to read it
		factoryScope.inTransaction( (session) -> {
			// reading it should be fine
			var entity = session.find( TheEntity.class, 1 );
			assertThat( entity.version ).isNull();

			// but later attempts to update it will fail
			entity.name = "third";

			try {
				session.flush();
				fail();
			}
			catch (Exception expected) {
			}
		} );
	}

	@Entity(name="TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		@Id
		private Integer id;
		private String name;
		@Version
		private Integer version;

		public TheEntity() {
		}

		public TheEntity(Integer id, String name, Integer version) {
			this.id = id;
			this.name = name;
			this.version = version;
		}
	}
}
