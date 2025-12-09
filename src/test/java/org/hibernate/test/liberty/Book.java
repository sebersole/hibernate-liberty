package org.hibernate.test.liberty;

import java.time.Instant;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Book")
@Table(name = "books")
@NamedNativeQuery(name = "book-complete",
		query = "select * from books",
		resultClass = Book.class)
@SqlResultSetMapping(name = "book-complete",
		entities = @EntityResult(entityClass = Book.class)
)
public class Book {
	@Id
	private Integer id;
	private String title;
	private String author;
	private Instant publishDate;
	@NaturalId
	private String isbn;

	public Book() {
	}

	public Book(Integer id, String title, String author, String isbn) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.isbn = isbn;
		this.publishDate = Instant.now();
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Instant getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Instant publishDate) {
		this.publishDate = publishDate;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
}
