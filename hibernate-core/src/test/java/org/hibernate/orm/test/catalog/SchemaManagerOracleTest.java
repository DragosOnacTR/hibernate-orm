/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = {SchemaManagerOracleTest.Book.class, SchemaManagerOracleTest.Author.class})
@SessionFactory(exportSchema = false)
@SkipForDialect(dialectClass = PostgreSQLDialect.class, reason = "doesn't work in the CI")
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsTruncateTable.class)
public class SchemaManagerOracleTest {

	@BeforeEach
	public void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().dropMappedObjects(false);
	}

	private Long countBooks(SessionImplementor s) {
		return s.createQuery("select count(*) from BookForTesting", Long.class).getSingleResult();
	}

	@Test public void test0(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		factory.getSchemaManager().exportMappedObjects(true);
		scope.inTransaction( s -> s.persist( new Book() ) );
		factory.getSchemaManager().validateMappedObjects();
		scope.inTransaction( s -> assertEquals( 1, countBooks(s) ) );
		factory.getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> assertEquals( 0, countBooks(s) ) );
		factory.getSchemaManager().dropMappedObjects(true);
		try {
			factory.getSchemaManager().validateMappedObjects();
			fail();
		}
		catch (SchemaManagementException e) {
			assertTrue( e.getMessage().contains("ForTesting") );
		}
	}

	@Entity(name="BookForTesting")
	static class Book {
		@Id String isbn = "xyz123";
		String title = "Hibernate in Action";
		@ManyToOne(cascade = CascadeType.PERSIST)
		Author author = new Author();
		{
			author.favoriteBook = this;
		}
	}

	@Entity(name="AuthorForTesting")
	static class Author {
		@Id String name = "Christian & Gavin";
		@ManyToOne
		public Book favoriteBook;
	}
}

