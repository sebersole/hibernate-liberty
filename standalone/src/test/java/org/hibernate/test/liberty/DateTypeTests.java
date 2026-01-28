package org.hibernate.test.liberty;

import java.sql.Types;
import java.util.Date;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = DateTypeTests.TheEntity.class)
@SessionFactory
public class DateTypeTests {
	@Test
	@AnalysisItem( id = 3, feature = "Missing @Temporal", description = "Using Date without precision",
			behavioralDifference = "EclipseLink may infer type; Hibernate throws if annotation is missing")
	void testDateTypeExport(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		final PersistentClass entityBinding = modelScope.getEntityBinding( TheEntity.class );

		final Property theDate = entityBinding.getProperty( "theDate" );
		final BasicValue.Resolution<?> theDateResolution = ( (BasicValue) theDate.getValue() ).resolve();
		assertThat( theDateResolution.getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.TIMESTAMP );

		final Property sqlDate = entityBinding.getProperty( "sqlDate" );
		final BasicValue.Resolution<?> sqlDateResolution = ( (BasicValue) sqlDate.getValue() ).resolve();
		assertThat( sqlDateResolution.getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.DATE );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "something") );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name="TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		@Id
		private Integer id;
		private String name;

		private Date theDate;
		private java.sql.Date sqlDate;

		public TheEntity() {
		}

		public TheEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
			theDate = new Date();
			sqlDate = new java.sql.Date(System.currentTimeMillis());
		}
	}
}
