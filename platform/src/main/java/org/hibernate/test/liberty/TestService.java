package org.hibernate.test.liberty;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

@Path("/")
@ApplicationScoped
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class TestService {

    @PersistenceContext(unitName = "HibernatePersistenceUnit")
    EntityManager entityManager;

    @Resource
    UserTransaction transaction;

    @GET
    @Path("/create")
    public int createPerson(@QueryParam("id") int id, @QueryParam("value") String value) throws Exception {
        Person p = new Person();
        p.id = id;
        p.value = value;

        transaction.begin();
        try {
            entityManager.persist(p);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }

        return p.id;
    }

    @GET
    @Path("/{id}")
    public Person getPerson(@PathParam("id") int id) {
        Person person = entityManager.find(Person.class, id);
        if (person == null) {
            throw new java.util.NoSuchElementException("Person with id " + id + " not found");
        }
        return person;
    }

    @GET
    @Path("/update")
    public Person updatePerson(@QueryParam("id") int id, @QueryParam("value") String newValue) throws Exception {
        Person person = getPerson(id);
        person.value = newValue;

        Person updated = null;

        transaction.begin();
        try {
            updated = entityManager.merge(person);
            transaction.commit();
            return updated;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }
}
