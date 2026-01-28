package org.hibernate.test.liberty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Person {
    @Id
    public int id;

    public String value;

    @Override
    public String toString() {
        return "Person id=" + id + " " + " value=" + value;
    }
}
