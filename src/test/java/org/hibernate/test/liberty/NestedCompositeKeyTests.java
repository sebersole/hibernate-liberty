package org.hibernate.test.liberty;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		NestedCompositeKeyTests.Level3.class,
		NestedCompositeKeyTests.Level2.class,
		NestedCompositeKeyTests.Level1.class,
		NestedCompositeKeyTests.Something.class,
})
@SessionFactory
public class NestedCompositeKeyTests {
	/// The assertion is very unclear.
	/// But here, we see Hibernate clearly supports nested embeddables for composite id.
	/// Waiting on the reporters for additional details.
	@Test
	@AnalysisItem( id = 6, feature = "Composite keys (@EmbeddedId)", description = "Nested embeddables in composite keys",
			behavioralDifference = "EclipseLink handles nested keys; Hibernate requires precise structure")
	void smokeTest(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		// force the export so we can see the schema
		factoryScope.getSessionFactory();

		// but here we are verifying
		final PersistentClass entityBinding = modelScope.getEntityBinding( Something.class );

		final Property identifierProperty = entityBinding.getIdentifierProperty();
		assertThat( identifierProperty.getPropertyAccessorName() ).isEqualTo( "field" );

		// Level1
		final Component firstLevel = (Component) entityBinding.getIdentifier();
		final Property level1Detail = firstLevel.getProperty( "level1Detail" );
		assertThat( level1Detail.getPropertyAccessorName() ).isEqualTo( "field" );
		final Property level2 = firstLevel.getProperty( "level2" );
		assertThat( level2.getPropertyAccessorName() ).isEqualTo( "field" );

		// Level2
		final Component secondLevel = (Component) level2.getValue();
		final Property level2Detail = secondLevel.getProperty( "level2Detail" );
		assertThat( level2Detail.getPropertyAccessorName() ).isEqualTo( "field" );
		final Property level3 = secondLevel.getProperty( "level3" );
		assertThat( level3.getPropertyAccessorName() ).isEqualTo( "field" );

		// Level3
		final Component thirdLevel = (Component) level3.getValue();
		final Property level3Detail1 = thirdLevel.getProperty( "level3Detail1" );
		assertThat( level3Detail1.getPropertyAccessorName() ).isEqualTo( "field" );
		final Property level3Detail2 = thirdLevel.getProperty( "level3Detail2" );
		assertThat( level3Detail2.getPropertyAccessorName() ).isEqualTo( "field" );
	}

	@Embeddable
	public static class Level1 {
		private String level1Detail;
		private Level2 level2;
	}

	@Embeddable
	public static class Level2 {
		private String level2Detail;
		private Level3 level3;
	}

	@Embeddable
	public static class Level3 {
		private String level3Detail1;
		private String level3Detail2;
	}

	@Entity(name="Something")
	@Table(name="somethings")
	public static class Something {
		@EmbeddedId
		private Level1 id;
		private String name;
	}
}
